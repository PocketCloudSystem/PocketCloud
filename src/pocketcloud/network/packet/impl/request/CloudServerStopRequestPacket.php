<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\RequestPacket;

class CloudServerStopRequestPacket extends RequestPacket {

    public function __construct(private string $requestId = "", private string $server = "") {
        parent::__construct($this->requestId);
    }

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