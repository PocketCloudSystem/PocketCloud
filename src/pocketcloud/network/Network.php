<?php

namespace pocketcloud\network;

use Exception;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\event\impl\network\NetworkBindEvent;
use pocketcloud\event\impl\network\NetworkCloseEvent;
use pocketcloud\event\impl\network\NetworkPacketReceiveEvent;
use pocketcloud\event\impl\network\NetworkPacketSendEvent;
use pocketcloud\language\Language;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\handler\PacketSerializer;
use pocketcloud\network\packet\UnhandledPacketObject;
use pocketcloud\PocketCloud;
use pocketcloud\thread\Thread;
use pocketcloud\util\Address;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\SingletonTrait;
use pocketmine\snooze\SleeperHandlerEntry;
use Socket;

class Network extends Thread {
    use SingletonTrait;

    private SleeperHandlerEntry $entry;
    private ThreadSafeArray $buffer;
    private Socket $socket;
    private bool $connected = false;

    public function __construct(private Address $address) {
        self::setInstance($this);
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
        CloudLogger::get()->info(Language::current()->translate("network.bind", $this->address->__toString()));
        if (!$this->bind($this->address)) {
            CloudLogger::get()->error(Language::current()->translate("network.bind.failed", $this->address->__toString()));
            PocketCloud::getInstance()->shutdown();
            return;
        } else {
            CloudLogger::get()->info(Language::current()->translate("network.bound", $this->address->__toString()));
        }

        $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function(): void {
            /** @var UnhandledPacketObject $object */
            while (($object = $this->buffer->shift()) !== null) {
                $buffer = $object->getBuffer();
                $address = new Address($object->getAddress(), $object->getPort());
                $client = ServerClientManager::getInstance()->getClientByAddress($address) ?? new ServerClient($address);
                $continue = true;
                if (DefaultConfig::getInstance()->isNetworkOnlyLocal() && !$address->isLocalHost()) $continue = false;
                if ($continue) {
                    try {
                        if (($packet = PacketSerializer::decode($buffer)) !== null) {
                            (new NetworkPacketReceiveEvent($packet, $client))->call();
                            $packet->handle($client);
                        } else CloudLogger::get()->warn(Language::current()->translate("network.receive.unknown", $client->getAddress()->__toString()))->debug(DefaultConfig::getInstance()->isNetworkEncryptionEnabled() ? base64_decode($buffer) : $buffer);
                    } catch (Exception $e) {
                        CloudLogger::get()->error("§cFailed to decode a packet!");
                        CloudLogger::get()->debug($buffer);
                        CloudLogger::get()->exception($e);
                    }
                } else CloudLogger::get()->warn(Language::current()->translate("network.receive.external", $client->getAddress()->__toString()));
            }
        });
    }

    private function bind(Address $address): bool {
        if ($this->connected) return false;
        $this->address = $address;
        $this->socket = @socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
        if(socket_bind($this->socket, $address->getAddress(), $address->getPort()) === true) {
            $this->connected = true;
            (new NetworkBindEvent($this->address))->call();
            socket_set_option($this->socket, SOL_SOCKET, SO_SNDBUF, 1024 * 1024 * 8);
            socket_set_option($this->socket, SOL_SOCKET, SO_RCVBUF, 1024 * 1024 * 8);
        } else return false;
        return true;
    }

    public function write(string $buffer, Address $dst): bool {
        if (!$this->isConnected()) return false;
        return socket_sendto($this->socket, $buffer, strlen($buffer), 0, $dst->getAddress(), $dst->getPort()) !== false;
    }

    public function read(?string &$buffer, ?string &$address, ?int &$port): bool {
        if (!$this->isConnected()) return false;
        return socket_recvfrom($this->socket, $buffer, 65535, 0, $address, $port) !== false;
    }

    public function close(): void {
        if ($this->isConnected()) {
            (new NetworkCloseEvent())->call();
            $this->connected = false;
            $this->quit();
        }
    }

    public function sendPacket(CloudPacket $packet, ServerClient $client): bool {
        $buffer = PacketSerializer::encode($packet);
        $success = $this->write($buffer, $client->getAddress());
        (new NetworkPacketSendEvent($packet, $client, $success))->call();
        return $success;
    }

    public function broadcastPacket(CloudPacket $packet, ServerClient... $excluded): void {
        foreach (ServerClientManager::getInstance()->getClients() as $client) {
            if (!in_array($client, $excluded)) $client->sendPacket(clone $packet);
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