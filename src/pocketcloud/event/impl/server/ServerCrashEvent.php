<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\server\CloudServer;

class ServerCrashEvent extends ServerEvent {

    public function __construct(private CloudServer $server, private array $data) {
        parent::__construct($this->server);
    }

    public function getData(): array {
        return $this->data;
    }
}