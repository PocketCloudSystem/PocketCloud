<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\network\Network;

abstract class NetworkEvent extends Event {

    public function __construct(protected readonly Network $network) {}

    public function getNetwork(): Network {
        return $this->network;
    }
}