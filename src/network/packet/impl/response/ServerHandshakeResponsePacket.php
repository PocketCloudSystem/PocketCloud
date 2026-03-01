<?php

namespace pocketcloud\cloud\network\packet\impl\response;

use pocketcloud\cloud\network\packet\data\VerifyStatus;
use pocketcloud\cloud\network\packet\ResponsePacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ServerHandshakeResponsePacket extends ResponsePacket {

    public function __construct(private readonly ?VerifyStatus $verifyStatus = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->verifyStatus);
    }

    public static function create(VerifyStatus $verifyStatus): self {
        return new self($verifyStatus);
    }
}