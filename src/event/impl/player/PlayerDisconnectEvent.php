<?php

namespace pocketcloud\cloud\event\impl\player;

use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\server\CloudServer;

class PlayerDisconnectEvent extends PlayerEvent {

    public function __construct(
        CloudPlayer $player,
        private readonly CloudServer $server
    ) {
        parent::__construct($player);
    }

    public function getServer(): CloudServer {
        return $this->server;
    }
}