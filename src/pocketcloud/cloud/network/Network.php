<?php

namespace pocketcloud\cloud\network;

use Exception;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\event\impl\network\NetworkBindEvent;
use pocketcloud\cloud\event\impl\network\NetworkCloseEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketReceiveEvent;
use pocketcloud\cloud\event\impl\network\NetworkPacketSendEvent;
use pocketcloud\cloud\network\packet\pool\PacketPool;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\traffic\impl\NetworkTrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\net\Address;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\handler\PacketSerializer;
use pocketcloud\cloud\network\packet\UnhandledPacketObject;
use pocketmine\snooze\SleeperHandlerEntry;
use Socket;

final class Network extends Thread {
    use SingletonTrait;

    private SleeperHandlerEntry $entry;
    private ThreadSafeArray $buffer;
    private Socket $socket;
    private bool $connected = false;

    public function __construct(private Address $address) {
        self::setInstance($this);
        PacketPool::init();
        $this->buffer = new ThreadSafeArray();
    }

    public function onRun(): void {
        while ($this->isConnected() && $this->isRunning()) {
            if ($this->read($buffer, $address, $port) !== false) {
                $this->buffer[] = new UnhandledPacketObject($buffer, $address, $port);
                $this->entry->createNotifier()->wakeupSleeper();
            }
        }
    }

    public function init(): void {
        CloudLogger::get()->info("Trying to bind to §b" . $this->address . "§r...");
        if (!$this->bind($this->address)) {
            CloudLogger::get()->error("§cFailed to bind to §e" . $this->address . "§c!");
            PocketCloud::getInstance()->shutdown();
            return;
        } else {
            CloudLogger::get()->success("Successfully bound to §b" . $this->address . "§r.");
        }

        $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function(): void {
            /** @var UnhandledPacketObject $object */
            while (($object = $this->buffer->shift()) !== null) {
                $buffer = $object->getBuffer();
                $address = new Address($object->getAddress(), $object->getPort());

                TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes = strlen($buffer), TrafficMonitor::REGULAR_MODE_IN);
                TrafficMonitorManager::getInstance()->callHandlers(
                    TrafficMonitorManager::TRAFFIC_NETWORK,
                    TrafficMonitor::REGULAR_MODE_IN,
                    $object->getBuffer(), $bytes, $address
                );

                $client = ServerClientCache::getInstance()->getByAddress($address) ?? new ServerClient($address);
                $continue = true;
                if (MainConfig::getInstance()->isNetworkOnlyLocal() && !$address->isLocal()) $continue = false;
                if ($continue) {
                    try {
                        if (($packet = PacketSerializer::decode($buffer)) !== null) {
                            TrafficMonitorManager::getInstance()->callHandlers(
                                TrafficMonitorManager::TRAFFIC_NETWORK,
                                NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_IN, $packet::class),
                                $packet, $address
                            );

                            new NetworkPacketReceiveEvent($packet, $client)->call();
                            $packet->handle($client);
                        } else CloudLogger::get()->warn("Received an unknown packet from §b" . $address . "§r, ignoring...")->debug("Packet buffer: " . (MainConfig::getInstance()->isNetworkEncryptionEnabled() ? base64_decode($buffer) : $buffer));
                    } catch (Exception $e) {
                        CloudLogger::get()->error("§cFailed to decode a packet!");
                        CloudLogger::get()->debug($buffer);
                        CloudLogger::get()->exception($e);
                    }
                } else CloudLogger::get()->warn("Received an external packet from §b" . $address . "§r, ignoring...")->debug("Packet buffer: " . $buffer);
            }
        });
    }

    private function bind(Address $address): bool {
        if ($this->connected) return false;
        $this->address = $address;
        $this->socket = @socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
        if(@socket_bind($this->socket, $address->getAddress(), $address->getPort()) === true) {
            $this->connected = true;
            new NetworkBindEvent($this->address)->call();
            socket_set_option($this->socket, SOL_SOCKET, SO_SNDBUF, 1024 * 1024 * 8);
            socket_set_option($this->socket, SOL_SOCKET, SO_RCVBUF, 1024 * 1024 * 8);
            socket_set_block($this->socket);
        } else return false;
        return true;
    }

    public function write(string $buffer, Address $dst): bool {
        if (!$this->isConnected()) return false;
        TrafficMonitorManager::getInstance()->pushBytes(TrafficMonitorManager::TRAFFIC_NETWORK, $bytes = strlen($buffer), TrafficMonitor::REGULAR_MODE_OUT);
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            TrafficMonitor::REGULAR_MODE_OUT,
            $buffer, $bytes, $dst
        );

        return @socket_sendto($this->socket, $buffer, $bytes, 0, $dst->getAddress(), $dst->getPort()) == strlen($buffer);
    }

    public function read(?string &$buffer, ?string &$address, ?int &$port): bool {
        if (!$this->isConnected()) return false;
        return @socket_recvfrom($this->socket, $buffer, 65535, 0, $address, $port) !== false;
    }

    public function close(): void {
        if ($this->isConnected()) {
            new NetworkCloseEvent()->call();
            $this->connected = false;
            $this->quit();
        }
    }

    public function sendPacket(CloudPacket $packet, ServerClient $client): bool {
        $buffer = PacketSerializer::encode($packet);
        $success = $this->write($buffer, $client->getAddress());
        TrafficMonitorManager::getInstance()->callHandlers(
            TrafficMonitorManager::TRAFFIC_NETWORK,
            NetworkTrafficMonitor::parsePacketMode(NetworkTrafficMonitor::NETWORK_MODE_PACKET_OUT, $packet::class),
            $packet, $client->getAddress(), $success
        );

        new NetworkPacketSendEvent($packet, $client, $success)->call();
        return $success;
    }

    public function broadcastPacket(CloudPacket $packet, ServerClient... $excluded): void {
        foreach (ServerClientCache::getInstance()->getAll() as $client) {
            if (!in_array($client, $excluded)) {
                $this->sendPacket(clone $packet, $client);
            }
        }
    }

    public function isConnected(): bool {
        return $this->connected;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}