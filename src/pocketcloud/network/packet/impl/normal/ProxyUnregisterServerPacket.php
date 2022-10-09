<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class ProxyUnregisterServerPacket extends CloudPacket {

    public function __construct(private string $serverName = "") {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->serverName);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->serverName = $content->readString();
    }

    public function getServerName(): string {
        return $this->serverName;
    }
}