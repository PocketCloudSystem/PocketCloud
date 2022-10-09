<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class ProxyRegisterServerPacket extends CloudPacket {

    public function __construct(private string $serverName = "", private int $port = 0) {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->serverName);
        $content->put($this->port);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->serverName = $content->readString();
        $this->port = $content->readInt();
    }

    public function getServerName(): string {
        return $this->serverName;
    }

    public function getPort(): int {
        return $this->port;
    }
}