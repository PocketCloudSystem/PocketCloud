<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\server\CloudServer;

class ServerCrashEvent extends ServerEvent {

    public function __construct(
        CloudServer $server,
        private readonly array $data
    ) {
        parent::__construct($server);
    }

    public function getData(): array {
        return $this->data;
    }
}