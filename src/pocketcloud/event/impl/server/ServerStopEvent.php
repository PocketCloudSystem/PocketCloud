<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\server\CloudServer;

class ServerStopEvent extends ServerEvent {

    public function __construct(private CloudServer $server, private bool $force) {
        parent::__construct($this->server);
    }

    public function isForce(): bool {
        return $this->force;
    }
}