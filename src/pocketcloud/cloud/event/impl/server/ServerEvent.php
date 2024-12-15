<?php

namespace pocketcloud\cloud\event\impl\server;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\server\CloudServer;

abstract class ServerEvent extends Event {

    public function __construct(private readonly CloudServer $server) {}

    public function getServer(): CloudServer {
        return $this->server;
    }
}