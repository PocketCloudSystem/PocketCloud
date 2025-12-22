<?php

namespace pocketcloud\cloud\event\impl\player;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\player\CloudPlayer;

abstract class PlayerEvent extends Event {

    public function __construct(private readonly CloudPlayer $player) {}

    public function getPlayer(): CloudPlayer {
        return $this->player;
    }
}