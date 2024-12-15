<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\server\CloudServerManager;

//sending to the sub servers
final class CloudSyncStoragesPacket extends CloudPacket {

    private array $storage = [];

    public function __construct() {
        foreach (CloudServerManager::getInstance()->getAll() as $server) {
            if (!$server->getInternalCloudServerStorage()->empty()) {
                $this->storage[$server->getName()] = $server->getInternalCloudServerStorage()->getAll();
            }
        }
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->storage);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->storage = $packetData->readArray();
    }

    public function getStorage(): array {
        return $this->storage;
    }

    public function handle(ServerClient $client): void {}

    public static function create(): self {
        return new self();
    }
}