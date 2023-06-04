<?php

namespace pocketcloud\event\impl\player;

use pocketcloud\player\CloudPlayer;
use pocketcloud\server\CloudServer;

class PlayerSwitchServerEvent extends PlayerEvent {

    public function __construct(private CloudPlayer $player, private ?CloudServer $oldServer, private CloudServer $newServer) {
        parent::__construct($this->player);
    }

    public function getOldServer(): ?CloudServer {
        return $this->oldServer;
    }

    public function getNewServer(): CloudServer {
        return $this->newServer;
    }
}