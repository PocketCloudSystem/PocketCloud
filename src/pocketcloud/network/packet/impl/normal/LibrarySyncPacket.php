<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\library\Library;
use pocketcloud\library\LibraryManager;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;

class LibrarySyncPacket extends CloudPacket {

    private array $data = [];

    public function __construct() {
        foreach (array_filter(LibraryManager::getInstance()->getLibraries(), fn(Library $library) => $library->isCloudBridgeOnly()) as $lib) {
            $this->data[] = [
                "name" => $lib->getName(),
                "path" => $lib->getUnzipLocation()
            ];
        }
    }

    public function encodePayload(PacketData $packetData) {
        $packetData->write($this->data);
    }

    public function decodePayload(PacketData $packetData) {
        $this->data = $packetData->readArray();
    }

    public function getData(): array {
        return $this->data;
    }

    public function handle(ServerClient $client) {}
}