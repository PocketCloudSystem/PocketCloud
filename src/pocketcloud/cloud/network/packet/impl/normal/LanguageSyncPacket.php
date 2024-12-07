<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\language\Language;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;

final class LanguageSyncPacket extends CloudPacket {

    private array $data = [];

    public function __construct() {
        /**
         * $this->data = [
         * "de_DE" => Language::GERMAN()->getMessages(),
         * "en_US" => Language::ENGLISH()->getMessages()
         * ];
         */
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