<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class LocalPlayerUpdatePacket extends CloudPacket {

    public function __construct(private string $player = "", private ?string $newServer = null) {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->player);
        $content->put($this->newServer);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->player = $content->readString();
        $this->newServer = $content->readString();
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function getNewServer(): ?string {
        return $this->newServer;
    }
}