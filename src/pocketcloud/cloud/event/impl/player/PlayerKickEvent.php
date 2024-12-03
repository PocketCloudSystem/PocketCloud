<?php

namespace pocketcloud\cloud\event\impl\player;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;
use pocketcloud\cloud\player\CloudPlayer;

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