<?php

namespace pocketcloud\cloud\network\client;

use Closure;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\net\Address;

final class ServerClient {

    private array $delayedPackets = [];

    public function __construct(private readonly Address $address) {}

    public function sendPacket(ClientboundPacket $packet): bool {
        if (!Network::getInstance()->sendPacket($packet, $this)) {
            CloudLogger::get()->warn("Failed to send packet §b{} §rto §b{}§r.", $packet->getName(), $this->address);
            return false;
        }
        return true;
    }

    /** @internal */
    public function unsetDelayedPacket(int $index): void {
        if (isset($this->delayedPackets[$index])) {
            unset($this->delayedPackets[$index]);
            $this->delayedPackets = array_values($this->delayedPackets);
        }
    }

    /**
     * @param CloudPacket $packet
     * @param int $ticks delay in ticks (20 = 1s)
     * @param Closure(ServerClient $client, CloudPacket $packet, bool $success): void|null $onSend
     * @return void
     */
    public function sendDelayedPacket(CloudPacket $packet, int $ticks, ?Closure $onSend = null): void {
        $this->delayedPackets[] = [$packet, $Server::getInstance()->tick + $ticks, $onSend];
    }

    public function getDelayedPackets(): array {
        return $this->delayedPackets;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function hasServer(): bool {
        return $this->getServer() !== null;
    }

    public function getServer(): ?CloudServer {
        return ServerClientCache::getInstance()->getServer($this);
    }

    public function toString(): string {
        return (string) $this;
    }

    public function __toString(): string {
        return "ServerClient[address=" . $this->getAddress() . "]";
    }
}