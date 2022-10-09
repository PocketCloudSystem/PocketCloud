<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;

class LocalTemplateUpdatePacket extends CloudPacket {

    public function __construct(private string $template = "", private array $newData = []) {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->template);
        $content->put($this->newData);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->template = $content->readString();
        $this->newData = $content->readArray();
    }

    public function getTemplate(): string {
        return $this->template;
    }

    public function getNewData(): ?array {
        return $this->newData;
    }
}