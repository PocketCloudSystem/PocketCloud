<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\player\CloudPlayer;

class PlayerDisconnectPacket extends CloudPacket {

    public function __construct(private ?CloudPlayer $player = null) {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->putPlayer($this->player);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->player = $content->readPlayer();
    }

    public function getPlayer(): ?CloudPlayer{
        return $this->player;
    }
}