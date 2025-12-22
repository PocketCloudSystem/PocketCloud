<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\util\PacketData;

interface Packet {

    public function encode(PacketData $packetData): void;

    public function encodePayload(PacketData $packetData): void;

    public function decode(PacketData $packetData): void;

    public function decodePayload(PacketData $packetData): void;

    public function handle(ServerClient $client): void;

    public function getName(): string;

    public function isEncoded(): bool;

    public function getSentTimestamp(): ?int;
}