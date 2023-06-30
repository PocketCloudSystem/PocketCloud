<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\player\CloudPlayer;
use pocketcloud\server\CloudServer;

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