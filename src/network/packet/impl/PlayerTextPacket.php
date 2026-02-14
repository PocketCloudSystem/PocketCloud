<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\TextType;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class PlayerTextPacket extends CloudPacket implements CloudboundPacket, ClientboundPacket {

    public function __construct(
        private string $player = "",
        private string $text = "",
        private ?TextType $type = null
    ) {}

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->player)) !== null) {
            $player->send($this->text, $this->type);
        }
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->player, $this->text, $this->type);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAllTypeSafe([&$this->player, &$this->text, &$this->type], [fn() => $packetData->readString(), fn() => $packetData->readString(), fn() => $packetData->readTextType()]);
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function getText(): string {
        return $this->text;
    }

    public function getType(): ?TextType {
        return $this->type;
    }

    public static function create(string $player, string $text, TextType $type): self {
        return new self($player, $text, $type);
    }
}