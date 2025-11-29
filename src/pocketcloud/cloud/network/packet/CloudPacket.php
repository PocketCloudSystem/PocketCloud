<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\server\CloudServer;
use ReflectionClass;
use RuntimeException;

abstract class CloudPacket  {

    private bool $encoded = false;

    public function encode(PacketData $packetData): void {
        if ($this->encoded) throw new RuntimeException("Packet: " . new ReflectionClass($this)->getShortName() . " is already encoded");
        $this->encoded = true;
        $packetData->write(new ReflectionClass($this)->getShortName());
        $this->encodePayload($packetData);
    }

    public function decode(PacketData $packetData): void {
        $packetData->readString();
        $this->decodePayload($packetData);
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {}

    abstract public function handle(ServerClient $client): void;

    public function sendPacket(ServerClient|CloudServer $client): bool {
        if ($client instanceof CloudServer) return $client->sendPacket($this);
        return Network::getInstance()->sendPacket($this, $client);
    }

    public function broadcastPacket(ServerClient ...$excluded): void {
        Network::getInstance()->broadcastPacket($this, ...$excluded);
    }

    public function isEncoded(): bool {
        return $this->encoded;
    }
}