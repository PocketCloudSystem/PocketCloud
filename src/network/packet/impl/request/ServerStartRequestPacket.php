<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ServerStartRequestPacket extends RequestPacket {

    public function __construct(
        private string $template = "",
        private int $count = 0
    ) {}

    public function handle(ServerClient $client): void {

    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->template, $this->count);
    }
}