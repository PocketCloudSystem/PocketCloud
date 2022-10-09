<?php

namespace pocketcloud\notify;

use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\NotifyPacket;
use pocketcloud\config\NotifyConfig;
use pocketcloud\player\CloudPlayerManager;

class Notifier {

    public static function sendNotify(NotifyMessage $message) {
        if (!$message->hasReplacements()) return;
        $players = [];

        foreach (CloudPlayerManager::getInstance()->getPlayers() as $player) if (NotifyConfig::getInstance()->is($player->getName())) $players[] = $player->getName();

        if (count($players) > 0) {
            Network::getInstance()->broadcastPacket(new NotifyPacket($message->parse($message->getReplacements()), $players));
            $message->resetReplacements();
        }
    }
}