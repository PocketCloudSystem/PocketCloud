<?php

namespace pocketcloud\cloud\network;

use JsonException;
use LogicException;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\network\NetworkPacketPreSendEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceiveEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceivePreProcessEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketSentEvent;
use pocketcloud\cloud\exception\PacketException;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\PacketPool;
use pocketcloud\cloud\network\packet\ResponseClientPacket;
use pocketcloud\cloud\network\packet\UnhandledPacket;
use pocketcloud\cloud\network\packet\util\PacketSerializer;
use pocketcloud\cloud\network\request\RequestManager;
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
    private ThreadSafeArray $sendBuffer;
    private ThreadSafeArray $disconnectBuffer;
    private SleeperHandlerEntry $handlerEntry;
    private Socket $serverSocket;
    private bool $established = false;
    private string $authenticationKey;

    public function __construct(private readonly Address $address) {
        self::setInstance($this);
        $this->buffer = new ThreadSafeArray();
        $this->sendBuffer = new ThreadSafeArray();
        $this->disconnectBuffer = new ThreadSafeArray();
        $this->authenticationKey = Utils::generateString(mt_rand(32, 64));

        PacketPool::init();

        $this->handlerEntry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            /** @var UnhandledPacket $unhandledPacket */
            while (($unhandledPacketData = $this->buffer->shift()) !== null) {
                if (!$this->established) return;
                [$address, $port, $buffer, $bytes] = $unhandledPacketData;
                $unhandledPacket = new UnhandledPacket($buffer, Address::create($address, $port), $bytes);
                $continue = true;

                TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes = $unhandledPacket->getBytes(), TrafficMonitor::REGULAR_MODE_IN);
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_NETWORK,
                    TrafficMonitor::REGULAR_MODE_IN,
                    $unhandledPacket->getBuffer(), $bytes, $unhandledPacket->getAddress()
                );

                $client = ServerClientCache::getInstance()->getByAddress($unhandledPacket->getAddress()) ?? new ServerClient($unhandledPacket->getAddress());

                ($ev = new NetworkPacketReceivePreProcessEvent($this, $client, $unhandledPacket->getBuffer(), $encryption = MainConfig::getInstance()->isNetworkEncryptionEnabled()))->call();
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

                            ($ev = new NetworkPacketReceiveEvent($this, $client, $packet))->call();
                            if ($ev->isCancelled()) return;
                            $packet->handle($client);

                            if ($packet instanceof ResponseClientPacket) {
                                RequestManager::getInstance()->resolve($packet);
                                RequestManager::getInstance()->remove($packet->getRequestId());
                            }
                        } else CloudLogger::get()->debug("Received an unknown packet from §b{}§r, ignoring...", $unhandledPacket->getAddress())->debug("Packet buffer: " . $unhandledPacket->getBuffer());
                    } catch (PacketException|JsonException $e) {
                        CloudLogger::get()->warn("§cFailed to decode packet from §b{}§8: §e{}", $client->getAddress(), $e->getMessage())
                            ->debug($unhandledPacket->getBuffer());
                        CloudLogger::get()->exception($e);
                    }
                } else CloudLogger::get()->debug("Received an external packet from §b{}§r, ignoring...", $unhandledPacket->getAddress())->debug("Packet buffer: " . $unhandledPacket->getBuffer());
            }

            while (($disconnectData = $this->disconnectBuffer->shift()) !== null) {
                if (!$this->established) return;
                [$address, $port] = $disconnectData;
                $addr = Address::create($address, $port);
                $client = ServerClientCache::getInstance()->getByAddress($addr);
                if ($client !== null) {
                    CloudLogger::get()->debug("TCP client §b{}§r disconnected.", $addr);
                }
            }
        });
    }

    public function init(): void {
        if ($this->established) throw new LogicException("Socket has already been established");
        $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
        if (!$socket) throw new RuntimeException(socket_strerror(socket_last_error()));
        $this->serverSocket = $socket;
        socket_set_option($socket, SOL_SOCKET, SO_REUSEADDR, 1);
        socket_set_option($socket, SOL_TCP, TCP_NODELAY, 1);
        if (socket_bind($socket, $this->address->getAddress(), $this->address->getPort())) {
            if (!socket_listen($socket)) {
                throw new RuntimeException(socket_strerror(socket_last_error()));
            }
            $this->established = true;
        } else throw new RuntimeException(socket_strerror(socket_last_error()));

        CloudLogger::get()->success("§bNetwork connection §rhas been §aestablished §ron §b{}§r.", $this->address);
        $this->start();
    }

    protected function onRun(): void {
        $notifier = $this->handlerEntry->createNotifier();
        /** @var array<Socket> $clientSockets  address_string => Socket */
        $clientSockets = [];
        /** @var array<string> $clientBuffers  address_string => raw bytes awaiting framing */
        $clientBuffers = [];

        while ($this->established && $this->isAlive()) {
            while (($item = $this->sendBuffer->shift()) !== null) {
                [$addrStr, $buffer] = $item;
                if (isset($clientSockets[$addrStr])) {
                    $this->tcpWrite($clientSockets[$addrStr], $buffer);
                }
            }

            $read = [$this->serverSocket, ...array_values($clientSockets)];
            $write = $except = [];

            if (socket_select($read, $write, $except, 0, 50 * 1000) < 1) continue;

            if (in_array($this->serverSocket, $read, true)) {
                $clientSocket = @socket_accept($this->serverSocket);
                if ($clientSocket !== false && $clientSocket !== null) {
                    socket_getpeername($clientSocket, $peerAddr, $peerPort);
                    $addrStr = $peerAddr . ":" . $peerPort;
                    $clientSockets[$addrStr] = $clientSocket;
                    $clientBuffers[$addrStr] = "";
                    CloudLogger::get()->debug("New TCP connection from §b{}§r.", $addrStr);
                }
            }

            foreach ($clientSockets as $addrStr => $clientSocket) {
                if (!in_array($clientSocket, $read, true)) continue;

                $chunk = "";
                $result = socket_recv($clientSocket, $chunk, 65535, 0);

                if ($result === 0 || $result === false) {
                    [$addr, $port] = explode(":", $addrStr, 2);
                    $this->disconnectBuffer[] = ThreadSafeArray::fromArray([$addr, (int) $port]);
                    socket_close($clientSocket);
                    unset($clientSockets[$addrStr], $clientBuffers[$addrStr]);
                    $notifier->wakeupSleeper();
                    continue;
                }

                $clientBuffers[$addrStr] .= $chunk;

                // Parse length-prefixed frames: [4-byte big-endian uint][payload]
                while (strlen($clientBuffers[$addrStr]) >= 4) {
                    $length = unpack("N", substr($clientBuffers[$addrStr], 0, 4))[1];
                    if (strlen($clientBuffers[$addrStr]) < 4 + $length) break;

                    $buffer = substr($clientBuffers[$addrStr], 4, $length);
                    $clientBuffers[$addrStr] = substr($clientBuffers[$addrStr], 4 + $length);

                    [$addr, $port] = explode(":", $addrStr, 2);
                    $this->buffer[] = ThreadSafeArray::fromArray([$addr, (int) $port, $buffer, $length]);
                    $notifier->wakeupSleeper();
                }
            }
        }

        foreach ($clientSockets as $sock) {
            socket_close($sock);
        }
    }

    /**
     * Write a length-prefixed frame to a connected TCP socket.
     * Loops until all bytes are flushed to handle partial writes.
     */
    private function tcpWrite(Socket $socket, string $buffer): bool {
        $framed = pack("N", strlen($buffer)) . $buffer;
        $total = strlen($framed);
        $sent = 0;
        while ($sent < $total) {
            $result = socket_write($socket, substr($framed, $sent), $total - $sent);
            if ($result === false) return false;
            $sent += $result;
        }
        return true;
    }

    public function sendPacket(ClientboundPacket $packet, ServerClient $client): bool {
        if (!$this->established) return false;
        ($ev = new NetworkPacketPreSendEvent($this, $client, $packet))->call();
        if ($ev->isCancelled()) return false;
        $buffer = PacketSerializer::encode($packet, MainConfig::getInstance()->isNetworkEncryptionEnabled(), $this->authenticationKey);
        if ($buffer === null) return false;
        $success = $this->write($buffer, $client->getAddress());

        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_OUT, $packet::class),
            $packet, $client->getAddress(), $success
        );

        new NetworkPacketSentEvent($this, $client, $packet, $success)->call();
        return $success;
    }

    public function broadcastPacket(ClientboundPacket $packet, ServerClient|TemplateType ...$exclusions): Promise {
        if (!$this->established) return Promise::all([]);
        $buffer = PacketSerializer::encode($packet, MainConfig::getInstance()->isNetworkEncryptionEnabled(), $this->authenticationKey);
        if ($buffer === null) return Promise::rejected("Buffer null");
        $promises = [];
        foreach (ServerClientCache::getInstance()->getAll() as $client) {
            if ($client->getServer() === null) continue;
            if (in_array($client, $exclusions) || in_array($client->getServer()->getTemplate()->getTemplateType(), $exclusions)) continue;
            ($ev = new NetworkPacketPreSendEvent($this, $client, $packet))->call();
            if ($ev->isCancelled()) {
                $success = false;
            } else {
                $success = $this->write($buffer, $client->getAddress());
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_NETWORK,
                    NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_OUT, $packet::class),
                    $packet, $client->getAddress(), $success
                );

                new NetworkPacketSentEvent($this, $client, $packet, $success)->call();
            }

            $promises[] = $success ? Promise::resolved() : Promise::rejected();
        }

        return Promise::all($promises);
    }

    /**
     * Enqueue a packet buffer for the background thread to send via TCP.
     * Traffic counters are updated here; actual socket I/O happens in onRun().
     */
    public function write(string $buffer, Address $dst, ?int &$bytes = null): bool {
        if (!$this->established) return false;
        $bytes = strlen($buffer);
        $this->sendBuffer[] = ThreadSafeArray::fromArray([$dst->toString(), $buffer]);

        TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes, TrafficMonitor::REGULAR_MODE_OUT);
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            TrafficMonitor::REGULAR_MODE_OUT,
            $buffer, $bytes, $dst
        );

        return true;
    }

    public function quit(): void {
        parent::quit();
        $this->buffer = new ThreadSafeArray();
        $this->sendBuffer = new ThreadSafeArray();
        $this->disconnectBuffer = new ThreadSafeArray();
    }

    public function close(): void {
        if (!$this->established) return;
        PocketCloud::getInstance()->getSleeperHandler()->removeNotifier($this->handlerEntry->getNotifierId());
        $this->buffer = new ThreadSafeArray();
        $this->sendBuffer = new ThreadSafeArray();
        $this->disconnectBuffer = new ThreadSafeArray();
        socket_close($this->serverSocket);
        $this->established = false;
    }

    public function getSocket(): Socket {
        return $this->serverSocket;
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