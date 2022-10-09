<?php

namespace pocketcloud\network\packet\impl\response;

use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\impl\types\ErrorReason;
use pocketcloud\network\packet\ResponsePacket;

class CloudServerStopResponsePacket extends ResponsePacket {

    public function __construct(private ?ErrorReason $errorReason = null, private string $requestId = "") {
        parent::__construct($this->requestId);
    }

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->putErrorReason($this->errorReason);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->errorReason = $content->readErrorReason();
    }

    public function getErrorReason(): ?ErrorReason {
        return $this->errorReason;
    }
}