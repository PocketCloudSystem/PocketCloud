<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class LocalServerUnregisterPacket extends CloudPacket {

    public function __construct(private string $server = "") {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->server);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->server = $content->readString();
    }

    public function getServer(): string {
        return $this->server;
    }
}