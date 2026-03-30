<?php

namespace pocketcloud\cloud\event\impl\network;


use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\client\ServerClient;

class NetworkPacketTooLargeEvent extends NetworkEvent {

    public function __construct(
        Network $network,
        protected readonly ServerClient $receiver,
        protected readonly ClientboundPacket $packet,
        protected readonly int $size,
        protected readonly string $buffer
    ) {
        parent::__construct($network);
    }

    public function getReceiver(): ServerClient {
        return $this->receiver;
    }

    public function getPacket(): ClientboundPacket {
        return $this->packet;
    }

    public function getSize(): int {
        return $this->size;
    }

    public function getBuffer(): string {
        return $this->buffer;
    }
}