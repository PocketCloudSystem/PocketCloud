<?php

namespace pocketcloud\cloud\network\packet\impl;

use JsonException;
use pocketcloud\cloud\language\Language;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class LanguageSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly string $language = "",
        private readonly array $messages = []
    ) {}

    public function handle(ServerClient $client): void {}

    /**
     * @throws JsonException
     */
    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->language, base64_encode(gzencode(json_encode($this->messages, JSON_THROW_ON_ERROR | JSON_INVALID_UTF8_IGNORE | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE))));
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getLanguage(): string {
        return $this->language;
    }

    public function getMessages(): array {
        return $this->messages;
    }

    public static function create(string $language, array $messages): self {
        return new self($language, $messages);
    }

    public static function fromLanguage(?Language $language = null): self {
        $language = $language ?? Language::current();
        return new self($language->getName(), $language->getMessages());
    }
}