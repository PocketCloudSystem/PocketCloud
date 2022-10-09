<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\RequestPacket;

class CheckPlayerNotifyRequestPacket extends RequestPacket {

    public function __construct(private string $requestId = "", private string $player = "") {
        parent::__construct($this->requestId);
    }

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