<?php

namespace pocketcloud\cloud\network\packet\impl\response;

use pocketcloud\cloud\network\packet\data\ServerErrorReason;
use pocketcloud\cloud\network\packet\ResponsePacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ServerSaveResponsePacket extends ResponsePacket {

    public function __construct(private readonly ?ServerErrorReason $errorReason = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->errorReason);
    }

    public function getErrorReason(): ?ServerErrorReason {
        return $this->errorReason;
    }

    public static function create(ServerErrorReason $errorReason): self {
        return new self($errorReason);
    }
}