<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\RequestPacket;

class CloudServerStartRequestPacket extends RequestPacket {

    public function __construct(private string $requestId = "", private string $template = "", private int $count = 0) {
        parent::__construct($this->requestId);
    }

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->template);
        $content->put($this->count);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->template = $content->readString();
        $this->count = $content->readInt();
    }

    public function getTemplate(): string {
        return $this->template;
    }

    public function getCount(): int {
        return $this->count;
    }
}