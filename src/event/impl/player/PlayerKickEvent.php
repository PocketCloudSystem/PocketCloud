<?php

namespace pocketcloud\cloud\event\impl\player;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;
use pocketcloud\cloud\player\CloudPlayer;

class PlayerKickEvent extends PlayerEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        CloudPlayer $player,
        protected readonly string $reason,
        protected readonly string $disconnectScreenMessage
    ) {
        parent::__construct($player);
    }

    public function getReason(): string {
        return $this->reason;
    }

    public function getDisconnectScreenMessage(): string {
        return $this->disconnectScreenMessage;
    }
}