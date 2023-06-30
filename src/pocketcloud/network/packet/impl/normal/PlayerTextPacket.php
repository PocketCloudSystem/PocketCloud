<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\types\TextType;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\player\CloudPlayerManager;

class PlayerTextPacket extends CloudPacket {

    public function __construct(
        private string $player = "",
        private string $message = "",
        private ?TextType $textType = null
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->player);
        $packetData->write($this->message);
        $packetData->writeTextType($this->textType);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->player = $packetData->readString();
        $this->message = $packetData->readString();
        $this->textType = $packetData->readTextType();
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function getMessage(): string {
        return $this->message;
    }

    public function getTextType(): TextType {
        return $this->textType;
    }

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->getPlayerByName($this->player)) !== null) $player->send($this->message, $this->textType);
    }
}