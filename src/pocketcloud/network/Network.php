<?php

namespace pocketcloud\network;

use pocketcloud\event\impl\network\NetworkBindEvent;
use pocketcloud\event\impl\network\NetworkCloseEvent;
use pocketcloud\event\impl\network\NetworkPacketSendEvent;
use pocketcloud\lib\snooze\SleeperNotifier;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\handler\encoder\PacketEncoder;
use pocketcloud\network\packet\handler\PacketHandler;
use pocketcloud\network\packet\UnhandledPacketObject;
use pocketcloud\PocketCloud;
use pocketcloud\thread\Thread;
use pocketcloud\utils\Address;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;

class Network extends Thread {
    use SingletonTrait;

    private SleeperNotifier $notifier;
    private \Threaded $buffer;
    private \Socket $socket;
    private bool $connected = false;

    public function __construct(private Address $address) {
        self::setInstance($this);
        $this->notifier = new SleeperNotifier();
        $this->buffer = new \Threaded();

        CloudLogger::get()->info("Try to bind to §e" . $this->address . "§r...");
        if (!$this->bind($this->address)) {
            CloudLogger::get()->error("§cCan't bind to §e" . $this->address . "§c!");
            PocketCloud::getInstance()->shutdown();
        } else {
            CloudLogger::get()->info("Successfully bound to §e" . $this->address . "§r!");
        }

        PocketCloud::getInstance()->getSleeperHandler()->addNotifier($this->notifier, function(): void {
            while(($object = $this->buffer->shift()) !== null) PacketHandler::getInstance()->handle($object);
        });
        $this->start();
    }

    public function run() {
        $this->registerClassLoader();
        while ($this->isConnected()) {
            if ($this->read($buffer, $address, $port) !== false) {
                $this->buffer[] = new UnhandledPacketObject($buffer, $address, $port);
                $this->notifier->wakeupSleeper();
            }
        }
    }

    private function bind(Address $address): bool {
        if ($this->connected) return false;
        $this->address = $address;
        $this->socket = @socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
        if(@socket_bind($this->socket, $address->getAddress(), $address->getPort()) === true) {
            $this->connected = true;
            (new NetworkBindEvent($this->address))->call();
            @socket_set_option($this->socket, SOL_SOCKET, SO_SNDBUF, 1024 * 1024 * 8);
            @socket_set_option($this->socket, SOL_SOCKET, SO_RCVBUF, 1024 * 1024 * 8);
        } else return false;
        return true;
    }

    public function write(string $buffer, Address $dst): bool {
        if (!$this->isConnected()) return false;
        return @socket_sendto($this->socket, $buffer, strlen($buffer), 0, $dst->getAddress(), $dst->getPort()) !== false;
    }

    public function read(?string &$buffer, ?string &$address, ?int &$port): bool {
        if (!$this->isConnected()) return false;
        return @socket_recvfrom($this->socket, $buffer, 65535, 0, $address, $port) !== false;
    }

    public function close() {
        if ($this->isConnected()) {
            (new NetworkCloseEvent())->call();
            $this->connected = false;
            @socket_close($this->socket);
        }
    }

    public function sendPacket(CloudPacket $packet, ServerClient $client): bool {
        $buffer = PacketEncoder::encode($packet);
        if ($buffer !== false) {
            $success = $this->write($buffer, $client->getAddress());
            (new NetworkPacketSendEvent($packet, $client, $success))->call();
            return $success;
        }
        return false;
    }

    public function broadcastPacket(CloudPacket $packet, ServerClient... $excluded) {
        foreach (ServerClientManager::getInstance()->getClients() as $client) {
            if (!in_array($client, $excluded)) $client->sendPacket($packet);
        }
    }

    public function isConnected(): bool {
        return $this->connected;
    }

    public function getAddress(): Address {
        return $this->address;
    }
}