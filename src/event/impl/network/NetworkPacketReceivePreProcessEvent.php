<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;
use pocketcloud\cloud\network\client\ServerClient;

final class NetworkPacketReceivePreProcessEvent extends NetworkEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        private readonly string $buffer,
        private readonly bool $encryption,
        ServerClient $client
    ) {
        parent::__construct($client);
    }

    public function getBuffer(): string {
        return $this->buffer;
    }

    public function isEncryption(): bool {
        return $this->encryption;
    }
}