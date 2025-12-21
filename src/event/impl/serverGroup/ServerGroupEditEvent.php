<?php

namespace pocketcloud\cloud\event\impl\serverGroup;

use pocketcloud\cloud\group\ServerGroup;

class ServerGroupEditEvent extends ServerGroupEvent {

    public function __construct(
        ServerGroup $serverGroup,
        private readonly array $newTemplates
    ) {
        parent::__construct($serverGroup);
    }

    public function getNewTemplates(): array {
        return $this->newTemplates;
    }
}