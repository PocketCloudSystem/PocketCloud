<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\event\impl\player\PlayerSwitchServerEvent;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\terminal\log\CloudLogger;

final class PlayerSwitchServerPacket extends CloudPacket {

    public function __construct(
        private string $playerName = "",
        private string $newServer = ""
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->playerName);
        $packetData->write($this->newServer);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->playerName = $packetData->readString();
        $this->newServer = $packetData->readString();
    }

    public function getPlayerName(): string {
        return $this->playerName;
    }

    public function getNewServer(): string {
        return $this->newServer;
    }

    public function handle(ServerClient $client): void {
        if (($player = CloudPlayerManager::getInstance()->get($this->playerName)) !== null) {
            if (($server = CloudServerManager::getInstance()->get($this->newServer)) !== null) {
                Network::getInstance()->broadcastPacket($this);
                CloudLogger::get()->info("Player %s performed a server switch (%s -> %s)", $player->getName(), ($player->getCurrentServer()?->getName() ?? "NULL"), ($server?->getName() ?? "NULL"));
                new PlayerSwitchServerEvent($player, $player->getCurrentServer(), $server)->call();
                $player->setCurrentServer($server);
            }
        }
    }

    public static function create(string $player, string $newServer): self {
        return new self($player, $newServer);
    }
}