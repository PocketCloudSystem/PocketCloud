<?php

namespace pocketcloud\player;

use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\LocalPlayerRegisterPacket;
use pocketcloud\network\packet\impl\normal\LocalPlayerUnregisterPacket;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;

class CloudPlayerManager {
    use SingletonTrait;

    /** @var array<CloudPlayer> */
    private array $players = [];

    public function addPlayer(CloudPlayer $player) {
        if ($player->getCurrentServer() === null) CloudLogger::get()->debug("Player " . $player->getName() . " is connected. (On: " . ($player->getCurrentProxy()?->getName() ?? "NULL") . ")");
        else CloudLogger::get()->debug("Player " . $player->getName() . " is connected. (On: " . ($player->getCurrentServer()?->getName() ?? "NULL") . ")");
        $this->players[$player->getName()] = $player;
        Network::getInstance()->broadcastPacket(new LocalPlayerRegisterPacket($player->toArray()));
    }

    public function removePlayer(CloudPlayer $player) {
        if ($player->getCurrentServer() === null) CloudLogger::get()->debug("Player " . $player->getName() . " is disconnected. (From: " . ($player->getCurrentProxy()?->getName() ?? "NULL") . ")");
        else CloudLogger::get()->debug("Player " . $player->getName() . " is disconnected. (From: " . ($player->getCurrentServer()?->getName() ?? "NULL") . ")");
        if (isset($this->players[$player->getName()])) unset($this->players[$player->getName()]);
        $player->setCurrentServer(null);
        $player->setCurrentProxy(null);
        Network::getInstance()->broadcastPacket(new LocalPlayerUnregisterPacket($player->getName()));
    }

    public function getPlayerByName(string $name): ?CloudPlayer {
        return $this->players[$name] ?? null;
    }

    public function getPlayerByUniqueId(string $uniqueId): ?CloudPlayer {
        return array_filter($this->players, fn(CloudPlayer $player) => $player->getUniqueId() == $uniqueId)[0] ?? null;
    }

    public function getPlayerByXboxUserId(string $xboxUserId): ?CloudPlayer {
        return array_filter($this->players, fn(CloudPlayer $player) => $player->getXboxUserId() == $xboxUserId)[0] ?? null;
    }

    public function getPlayers(): array {
        return $this->players;
    }
}