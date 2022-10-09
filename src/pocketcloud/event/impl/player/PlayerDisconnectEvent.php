<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\player\CloudPlayer;
use pocketcloud\server\CloudServer;

class PlayerDisconnectEvent extends PlayerEvent {

    public function __construct(private CloudPlayer $player, private CloudServer $server) {
        parent::__construct($this->player);
    }

    public function getServer(): CloudServer {
        return $this->server;
    }
}