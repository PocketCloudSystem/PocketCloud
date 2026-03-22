<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;

final class NetworkPacketReceivePreProcessEvent extends NetworkEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        Network $network,
        protected readonly ServerClient $sender,
        protected readonly string $buffer,
        protected readonly bool $encryption
    ) {
        parent::__construct($network);
    }

    public function getSender(): ServerClient {
        return $this->sender;
    }

    public function getBuffer(): string {
        return $this->buffer;
    }

    public function isEncryption(): bool {
        return $this->encryption;
    }
}