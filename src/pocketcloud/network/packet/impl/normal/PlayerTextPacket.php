<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\content\PacketContent;
use pocketcloud\network\packet\impl\types\TextType;

class PlayerTextPacket extends CloudPacket {

    public function __construct(private string $player = "", private string $message = "", private ?TextType $textType = null) {}

    protected function encodePayload(PacketContent $content): void {
        parent::encodePayload($content);
        $content->put($this->player);
        $content->put($this->message);
        $content->putTextType($this->textType);
    }

    protected function decodePayload(PacketContent $content): void {
        parent::decodePayload($content);
        $this->player = $content->readString();
        $this->message = $content->readString();
        $this->textType = $content->readTextType();
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function getMessage(): string {
        return $this->message;
    }

    public function getTextType(): TextType {
        return $this->textType;
    }
}