<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;

final class LanguageSyncPacket extends CloudPacket {

    public function __construct(
        private string $language = "",
        private array $messages = []
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->language)
            ->write($this->messages);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->language = $packetData->readString();
        $this->messages = $packetData->readArray();
    }

    public function getLanguage(): string {
        return $this->language;
    }

    public function getMessages(): array {
        return $this->messages;
    }

    public function handle(ServerClient $client): void {}

    public static function create(string $language, array $messages): self {
        return new self($language, $messages);
    }
}