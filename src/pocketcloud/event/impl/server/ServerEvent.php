<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\event\Event;
use pocketcloud\server\CloudServer;

abstract class ServerEvent extends Event {

    public function __construct(private CloudServer $server) {}

    public function getServer(): CloudServer {
        return $this->server;
    }
}