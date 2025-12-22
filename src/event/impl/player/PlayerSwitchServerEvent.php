<?php

namespace pocketcloud\cloud\event\impl\player;

use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\server\CloudServer;

class PlayerSwitchServerEvent extends PlayerEvent {

    public function __construct(
        CloudPlayer $player,
        private readonly ?CloudServer $oldServer,
        private readonly CloudServer $newServer
    ) {
        parent::__construct($player);
    }

    public function getOldServer(): ?CloudServer {
        return $this->oldServer;
    }

    public function getNewServer(): CloudServer {
        return $this->newServer;
    }
}