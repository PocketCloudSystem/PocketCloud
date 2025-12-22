<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;

abstract class NetworkEvent extends Event {

    public function __construct(
        private readonly ServerClient $client
    ) {}

    public function getClient(): ServerClient {
        return $this->client;
    }
}