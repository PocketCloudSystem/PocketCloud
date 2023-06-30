<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\event\Cancelable;
use pocketcloud\event\CancelableTrait;
use pocketcloud\player\CloudPlayer;

class PlayerKickEvent extends PlayerEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        CloudPlayer $player,
        private readonly string $reason
    ) {
        parent::__construct($player);
    }

    public function getReason(): string {
        return $this->reason;
    }
}