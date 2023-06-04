<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\event\impl\player\PlayerSwitchServerEvent;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\CloudServerManager;
use pocketcloud\util\CloudLogger;

class PlayerSwitchServerPacket extends CloudPacket {

    public function __construct(private string $playerName = "", private string $newServer = "") {}

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

    public function handle(ServerClient $client) {
        if (($player = CloudPlayerManager::getInstance()->getPlayerByName($this->playerName)) !== null) {
            if (($server = CloudServerManager::getInstance()->getServerByName($this->newServer)) !== null) {
                Network::getInstance()->broadcastPacket($this);
                CloudLogger::get()->debug("Player %s performed a server switch (%s -> %s)", false, $player->getName(), ($player->getCurrentServer()?->getName() ?? "NULL"), ($server?->getName() ?? "NULL"));
                (new PlayerSwitchServerEvent($player, $player->getCurrentServer(), $server));
                $player->setCurrentServer($server);
            }
        }
    }
}