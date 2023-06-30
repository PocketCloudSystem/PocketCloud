<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\player\CloudPlayer;
use pocketcloud\server\CloudServer;

class PlayerConnectEvent extends PlayerEvent {

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