<?php

namespace pocketcloud\network\packet;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\content\PacketContent;

class RequestPacket extends CloudPacket {

    public function __construct(private string $requestId) {}

    public function encode(PacketContent $content): void {
        parent::encode($content);
        $content->put($this->requestId);
    }

    public function decode(PacketContent $content): void {
        parent::decode($content);
        $this->requestId = $content->readString();
    }

    public function sendResponse(ResponsePacket $packet, ServerClient $client) {
        $client->sendPacket($packet->setRequestId($this->requestId));
    }

    public function getRequestId(): string {
        return $this->requestId;
    }
}