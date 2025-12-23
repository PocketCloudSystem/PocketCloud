<?php

namespace pocketcloud\cloud\event\impl\server;

use pocketcloud\cloud\server\CloudServer;

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