<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\server\status\ServerStatus;

class CloudServerStatusChangePacket extends CloudPacket {

    public function __construct(private ?ServerStatus $newStatus = null) {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->putServerStatus($this->newStatus);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->newStatus = $content->readServerStatus();
    }

    public function getNewStatus(): ?ServerStatus {
        return $this->newStatus;
    }
}