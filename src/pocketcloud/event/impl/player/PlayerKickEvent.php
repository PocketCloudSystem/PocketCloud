<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\event\Cancelable;
use pocketcloud\event\CancelableTrait;
use pocketcloud\player\CloudPlayer;

class PlayerKickEvent extends PlayerEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(private CloudPlayer $player, private string $reason) {
        parent::__construct($this->player);
    }

    public function getReason(): string {
        return $this->reason;
    }
}