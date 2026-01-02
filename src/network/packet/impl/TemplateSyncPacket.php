<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\template\Template;

final class TemplateSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly ?Template $template = null,
        private readonly bool $removal = false
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->template, $this->removal);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getTemplate(): ?Template {
        return $this->template;
    }

    public function isRemoval(): bool {
        return $this->removal;
    }

    public static function create(Template $template, bool $removal): self {
        return new self($template, $removal);
    }
}