<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\player\PlayerSwitchServerEvent;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;

final class PlayerSwitchServerPacket extends CloudPacket implements CloudboundPacket {

    public function __construct(
        private string $player = "",
        private string $newServer = ""
    ) {}

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->player)) !== null) {
            if (($server = CloudServerManager::getInstance()->get($this->newServer)) !== null) {
                CloudLogger::get()->info("Player §b{} performed a server switch from §b{} §rto §b{}§r.", $player->getName(), ($player->getCurrentServer()?->getName() ?? "NULL"), ($server?->getName() ?? "NULL"));
                new PlayerSwitchServerEvent($player, $player->getCurrentServer(), $server)->call();
                $player->setCurrentServer($server);
            }
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->player, $this->newServer);
    }

    public static function create(string $player, string $reason): self {
        return new self($player, $reason);
    }
}