<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\cache\impl\NotificationListCache;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\impl\response\PlayerNotificationCheckResponsePacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class PlayerNotificationCheckRequestPacket extends RequestPacket {

    public function __construct(private string $player = "") {}

    public function handle(ServerClient $client): void {
        $this->sendResponse(PlayerNotificationCheckResponsePacket::create(NotificationListCache::is($this->player)), $client);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->player);
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public static function create(string $player): self {
        return new self($player);
    }
}