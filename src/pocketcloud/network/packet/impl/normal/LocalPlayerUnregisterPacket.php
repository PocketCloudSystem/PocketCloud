<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class LocalPlayerUnregisterPacket extends CloudPacket {

    public function __construct(private string $player = "") {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->player);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->player = $content->readString();
    }

    public function getPlayer(): string {
        return $this->player;
    }
}