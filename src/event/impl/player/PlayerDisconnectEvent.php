<?php

namespace pocketcloud\cloud\event\impl\player;

use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\server\CloudServer;

class PlayerDisconnectEvent extends PlayerEvent {

    public function __construct(
        CloudPlayer $player,
        protected readonly ?CloudServer $server,
        protected readonly string $serverName
    ) {
        parent::__construct($player);
    }

    public function getServer(): CloudServer {
        return $this->server;
    }

    public function getServerName(): string {
        return $this->serverName;
    }
}