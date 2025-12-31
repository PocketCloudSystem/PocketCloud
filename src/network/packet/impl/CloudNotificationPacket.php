<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CloudNotificationPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(private readonly string $message = "") {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->message);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getMessage(): string {
        return $this->message;
    }

    public static function create(string $message): self {
        return new self($message);
    }
}