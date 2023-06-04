<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\config\ModuleConfig;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;

class ModuleSyncPacket extends CloudPacket {

    private array $data;

    public function __construct() {
        $this->data = [
            "sign" => ["enabled" => ModuleConfig::getInstance()->isSignModule()],
            "npc" => ["enabled" => ModuleConfig::getInstance()->isNpcModule()],
            "global_chat" => ["enabled" => ModuleConfig::getInstance()->isGlobalChatModule()],
            "hub_command" => ["enabled" => ModuleConfig::getInstance()->isHubCommandModule()],
        ];
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