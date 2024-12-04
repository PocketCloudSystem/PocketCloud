<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\library\Library;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;

class LibrarySyncPacket extends CloudPacket {

    private array $data = [];

    public function __construct() {
        foreach (array_filter(LibraryManager::getInstance()->getAll(), fn(Library $library) => $library->isCloudBridgeOnly()) as $lib) {
            $this->data[] = [
                "name" => $lib->getName(),
                "path" => $lib->getUnzipLocation()
            ];
        }
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->data);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->data = $packetData->readArray();
    }

    public function getData(): array {
        return $this->data;
    }

    public function handle(ServerClient $client): void {}

    public static function create(): self {
        return new self();
    }
}