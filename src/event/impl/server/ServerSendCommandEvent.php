<?php

namespace pocketcloud\cloud\event\impl\server;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;
use pocketcloud\cloud\server\CloudServer;

class ServerSendCommandEvent extends ServerEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        CloudServer $server,
        private readonly string $commandLine
    ) {
        parent::__construct($server);
    }

    public function getCommandLine(): string {
        return $this->commandLine;
    }
}