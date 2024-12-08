<?php

namespace pocketcloud\cloud\event\impl\serverGroup;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\group\ServerGroup;

abstract class ServerGroupEvent extends Event {

    public function __construct(private readonly ServerGroup $serverGroup) {}

    public function getServerGroup(): ServerGroup {
        return $this->serverGroup;
    }
}