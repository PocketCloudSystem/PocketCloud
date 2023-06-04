<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\event\Cancelable;
use pocketcloud\event\CancelableTrait;
use pocketcloud\server\CloudServer;

class ServerSendCommandEvent extends ServerEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(private CloudServer $server, private string $commandLine) {
        parent::__construct($this->server);
    }

    public function getCommandLine(): string {
        return $this->commandLine;
    }
}