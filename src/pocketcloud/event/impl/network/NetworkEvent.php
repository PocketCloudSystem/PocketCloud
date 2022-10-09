<?php

namespace pocketcloud\event\impl\network;

use pocketcloud\event\Event;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;

abstract class NetworkEvent extends Event {

    public function __construct(private CloudPacket $packet, private ServerClient $client) {}

    public function getPacket(): CloudPacket {
        return $this->packet;
    }

    public function getClient(): ServerClient {
        return $this->client;
    }
}