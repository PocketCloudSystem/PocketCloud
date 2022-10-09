<?php

namespace pocketcloud\config;

use pocketcloud\network\packet\impl\normal\PlayerKickPacket;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;

class MaintenanceConfig {
    use SingletonTrait;

    private Config $config;

    public function __construct() {
        self::setInstance($this);
        $this->config = new Config(IN_GAME_PATH . "maintenanceList.json", 1);
    }

    public function add(string $player) {
        $this->config->set($player, true);
        $this->config->save();
        $this->config->reload();
    }

    public function edit(string $player, bool $v) {
        $this->config->set($player, $v);
        $this->config->save();
        $this->config->reload();
    }

    public function remove(string $player) {
        $this->config->remove($player);
        $this->config->save();
        $this->config->reload();

        if (($player = CloudPlayerManager::getInstance()->getPlayerByName($player)) !== null) {
            if ($player->getCurrentServer() !== null) {
                if ($player->getCurrentServer()->getTemplate()->isMaintenance()) $player->getCurrentServer()->sendPacket(new PlayerKickPacket($player->getName(), "MAINTENANCE"));
            }
        }
    }

    public function is(string $player): bool {
        return ($this->config->exists($player) ? $this->config->get($player, false) : false);
    }

    public function reload(): void {
        $this->config->reload();
    }

    public function getConfig(): Config {
        return $this->config;
    }
}