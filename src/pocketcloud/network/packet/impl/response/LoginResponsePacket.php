<?php

namespace pocketcloud\network\packet\impl\response;

use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\network\packet\ResponsePacket;

class LoginResponsePacket extends ResponsePacket {

    public function __construct(private ?VerifyStatus $verifyStatus = null, private string $requestId = "") {
        parent::__construct($this->requestId);
    }

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->putVerifyStatus($this->verifyStatus);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->verifyStatus = $content->readVerifyStatus();
    }

    public function getVerifyStatus(): VerifyStatus {
        return $this->verifyStatus;
    }
}