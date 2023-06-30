<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\server\CloudServer;

class ServerStopEvent extends ServerEvent {

    public function __construct(
        CloudServer $server,
        private readonly bool $force
    ) {
        parent::__construct($server);
    }

    public function isForce(): bool {
        return $this->force;
    }
}