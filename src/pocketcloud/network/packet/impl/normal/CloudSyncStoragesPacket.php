<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\server\CloudServerManager;

//sending to the sub servers
class CloudSyncStoragesPacket extends CloudPacket {

    private array $storage = [];

    public function __construct() {
        foreach (CloudServerManager::getInstance()->getServers() as $server) {
            if (!$server->getCloudServerStorage()->empty()) {
                $this->storage[$server->getName()] = $server->getCloudServerStorage()->getStorage();
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
}