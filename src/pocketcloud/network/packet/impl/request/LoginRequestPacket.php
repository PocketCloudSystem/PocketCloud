<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\RequestPacket;

class LoginRequestPacket extends RequestPacket {

    public function __construct(private string $requestId = "", private string $serverName = "", private int $processId = 0) {
        parent::__construct($this->requestId);
    }

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->serverName);
        $content->put($this->processId);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->serverName = $content->readString();
        $this->processId = $content->readInt();
    }

    public function getServerName(): string {
        return $this->serverName;
    }

    public function getProcessId(): int {
        return $this->processId;
    }
}