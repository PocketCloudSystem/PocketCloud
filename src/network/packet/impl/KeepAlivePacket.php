<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class KeepAlivePacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->setLastCheckTime(time());
            $server->sendDelayedPacket(new KeepAlivePacket(), ($server->getTemplate()->getTemplateType()->getServerTimeout() / 2) * 20);
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {}

    public static function create(): self {
        return new self();
    }
}