<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class PlayerKickPacket extends CloudPacket {

    public function __construct(private string $player = "", private string $reason = "") {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->player);
        $content->put($this->reason);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->player = $content->readString();
        $this->reason = $content->readString();
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function getReason(): string {
        return $this->reason;
    }
}