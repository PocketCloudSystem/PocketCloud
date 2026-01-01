<?php

namespace pocketcloud\cloud\network;

use ErrorException;
use JsonException;
use LogicException;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\network\NetworkPacketPreSendEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceiveEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceivePreProcessEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketSendEvent;
use pocketcloud\cloud\exception\PacketException;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\PacketPool;
use pocketcloud\cloud\network\packet\UnhandledPacket;
use pocketcloud\cloud\network\packet\util\PacketSerializer;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\traffic\impl\NetworkTrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use pocketmine\snooze\SleeperHandlerEntry;
use RuntimeException;
use Socket;

final class Network extends Thread {
    use SingletonTrait;

    private ThreadSafeArray $buffer;
    private SleeperHandlerEntry $handlerEntry;
    private Socket $socket;
    private bool $established = false;
    private string $authenticationKey;

    public function __construct(private readonly Address $address) {
        self::setInstance($this);
        $this->buffer = new ThreadSafeArray();
        $this->authenticationKey = Utils::generateString(mt_rand(32, 64));

        PacketPool::init();

        $this->handlerEntry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            /** @var UnhandledPacket $unhandledPacket */
            while (($unhandledPacket = $this->buffer->shift()) !== null) {
                $continue = true;

                TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes = $unhandledPacket->getBytes(), TrafficMonitor::REGULAR_MODE_IN);
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_NETWORK,
                    TrafficMonitor::REGULAR_MODE_IN,
                    $unhandledPacket->getBuffer(), $bytes, $unhandledPacket->getAddress()
                );

                $client = ServerClientCache::getInstance()->getByAddress($unhandledPacket->getAddress()) ?? new ServerClient($unhandledPacket->getAddress());

                ($ev = new NetworkPacketReceivePreProcessEvent($unhandledPacket->getBuffer(), $encryption = MainConfig::getInstance()->isNetworkEncryptionEnabled(), $client))->call();
                if ($ev->isCancelled()) return;

                if (MainConfig::getInstance()->isNetworkOnlyLocal() && !$unhandledPacket->getAddress()->isLocal()) $continue = false;
                if ($continue) {
                    try {
                        if (($packet = $unhandledPacket->buildCloudPacket($encryption, $this->authenticationKey)) !== null) {
                            TrafficMonitorManager::getInstance()->callHandlers(
                                TrafficMonitorManager::TRAFFIC_NETWORK,
                                NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_IN, $packet::class),
                                $packet, $unhandledPacket->getAddress()
                            );

                            ($ev = new NetworkPacketReceiveEvent($packet, $client))->call();
                            if (!$ev->isCancelled()) $packet->handle($client);
                        } else CloudLogger::get()->debug("Received an unknown packet from §b{}§r, ignoring...", $unhandledPacket->getAddress())->debug("Packet buffer: " . $unhandledPacket->getBuffer());
                    } catch (PacketException|JsonException $e) {
                        CloudLogger::get()->warn("§cFailed to decode packet from §b{}§8: §e{}", $client->getAddress(), $e->getMessage())
                            ->debug($unhandledPacket->getBuffer());
                    }
                } else CloudLogger::get()->debug("Received an external packet from §b{}§r, ignoring...", $unhandledPacket->getAddress())->debug("Packet buffer: " . $unhandledPacket->getBuffer());
            }
        });
    }

    public function init(): void {
        if ($this->established) throw new LogicException("Socket has already been established");
        $socket = @socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
        if (!$socket) throw new RuntimeException(socket_strerror(socket_last_error()));
        $this->socket = $socket;
        if (@socket_bind($socket, $this->address->getAddress(), $this->address->getPort())) {
            $this->established = true;
            socket_set_option($this->socket, SOL_SOCKET, SO_SNDBUF, 1024 * 1024 * 8);
            socket_set_option($this->socket, SOL_SOCKET, SO_RCVBUF, 1024 * 1024 * 8);
        } else throw new RuntimeException(socket_strerror(socket_last_error()));

        CloudLogger::get()->success("§bNetwork connection §rhas been §aestablished §ron §b{}§r.", $this->address);
        $this->start();
    }

    protected function onRun(): void {
        while ($this->established && $this->isAlive()) {
            $read = [$this->socket];
            $write = $except = [];

            if (socket_select($read, $write, $except, 0, 50 * 1000) > 0) {
                if ($this->read($bytes, $buffer, $address, $port)) {
                    $this->buffer[] = new UnhandledPacket($buffer, Address::create($address, $port), $bytes);
                    $this->handlerEntry->createNotifier()->wakeupSleeper();
                }
            }
        }
    }

    public function sendPacket(ClientboundPacket $packet, ServerClient $client): bool {
        if (!$this->established) return false;
        ($ev = new NetworkPacketPreSendEvent($packet, $client))->call();
        if ($ev->isCancelled()) return false;
        $buffer = PacketSerializer::encode($packet, MainConfig::getInstance()->isNetworkEncryptionEnabled(), $this->authenticationKey);
        if ($buffer === null) return false;
        $success = $this->write($buffer, $client->getAddress());
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_OUT, $packet::class),
            $packet, $client->getAddress(), $success
        );

        new NetworkPacketSendEvent($packet, $client, $success)->call();
        return $success;
    }

    public function broadcastPacket(ClientboundPacket $packet, ServerClient|TemplateType ...$exclusions): Promise {
        if (!$this->established) return Promise::all([]);
        $buffer = PacketSerializer::encode($packet, MainConfig::getInstance()->isNetworkEncryptionEnabled(), $this->authenticationKey);
        if ($buffer === null) return Promise::rejected("Buffer null");
        $promises = [];
        foreach (ServerClientCache::getInstance()->getAll() as $client) {
            if (in_array($client, $exclusions) || in_array($client->getServer()->getTemplate()->getTemplateType(), $exclusions)) continue;
            ($ev = new NetworkPacketPreSendEvent($packet, $client))->call();
            if ($ev->isCancelled()) {
                $success = false;
            } else {
                $success = $this->write($buffer, $client->getAddress());
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_NETWORK,
                    NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_OUT, $packet::class),
                    $packet, $client->getAddress(), $success
                );

                new NetworkPacketSendEvent($packet, $client, $success)->call();
            }

            $promises[] = $success ? Promise::resolved() : Promise::rejected();
        }

        return Promise::all($promises);
    }

    public function write(string $buffer, Address $dst): bool {
        if (!$this->established) return false;
        $sent = socket_sendto($this->socket, $buffer, $bytes = strlen($buffer), 0, $dst->getAddress(), $dst->getPort());
        if ($sent === false) return false;

        TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $sent, TrafficMonitor::REGULAR_MODE_OUT);
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            TrafficMonitor::REGULAR_MODE_OUT,
            $buffer,
            $sent,
            $dst
        );

        return $sent === $bytes;
    }

    public function read(?int &$bytes, ?string &$buffer, ?string &$address, ?int &$port): bool {
        if (!$this->established) return false;
        $result = socket_recvfrom($this->socket, $buffer, 65535, 0, $address, $port);
        if ($result === false) {
            $bytes = 0;
            return false;
        }

        $bytes = $result;
        return true;
    }

    public function close(): void {
        if (!$this->established) return;
        @socket_close($this->socket);
        $this->established = false;
    }

    public function getSocket(): Socket {
        return $this->socket;
    }

    public function isEstablished(): bool {
        return $this->established;
    }

    public function getAuthenticationKey(): string {
        return $this->authenticationKey;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public static function getInstance(): self {
        return self::$instance;
    }
}