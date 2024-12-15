<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;

//coming from sub server
final class CloudServerSyncStoragePacket extends CloudPacket {

    public function __construct(private array $data = []) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->data);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->data = $packetData->readArray();
    }

    public function getData(): array {
        return $this->data;
    }

    public function handle(ServerClient $client): void {
        $client->getServer()?->getInternalCloudServerStorage()->sync($this->data);
        Network::getInstance()->broadcastPacket(new CloudSyncStoragesPacket());
    }

    public static function create(array $data): self {
        return new self($data);
    }
}