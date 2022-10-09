<?php

namespace pocketcloud\network\packet;

use pocketcloud\network\packet\content\PacketContent;

abstract class CloudPacket {

    public function encode(PacketContent $content): void {
        $content->put($this->getIdentifier());
        $this->encodePayload($content);
    }

    public function decode(PacketContent $content): void {
        $content->read();
        $this->decodePayload($content);
    }

    protected function encodePayload(PacketContent $content): void {}

    protected function decodePayload(PacketContent $content): void {}

    public function getIdentifier(): string {
        return (new \ReflectionClass($this))->getShortName();
    }
}