<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\event\Event;
use pocketcloud\player\CloudPlayer;

abstract class PlayerEvent extends Event {

    public function __construct(private readonly CloudPlayer $player) {}

    public function getPlayer(): CloudPlayer {
        return $this->player;
    }
}