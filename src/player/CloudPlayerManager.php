<?php

namespace pocketcloud\cloud\player;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\player\PlayerConnectEvent;
use pocketcloud\cloud\event\impl\player\PlayerDisconnectEvent;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class CloudPlayerManager {
    use SingletonTrait;

    /** @var array<CloudPlayer> */
    private array $players = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function add(CloudPlayer $player): void {
        CloudLogger::get()->info("Player {} has connected via {}.", $player->getName(), ($player->getCurrentProxyName() ?? $player->getCurrentServerName()) ?? "NULL");
        $this->players[$player->getName()] = $player;
        #PlayerSyncPacket::create($player, false)->broadcastPacket();

        new PlayerConnectEvent($player, ($player->getCurrentServer() ?? $player->getCurrentProxy()))->call();
    }

    public function remove(CloudPlayer $player): void {
        CloudLogger::get()->info("Player {} disconnected from {}.", $player->getName(), ($player->getCurrentServerName() ?? $player->getCurrentProxyName()) ?? "NULL");

        if (isset($this->players[$player->getName()])) unset($this->players[$player->getName()]);
        new PlayerDisconnectEvent($player, ($player->getCurrentServer() ?? $player->getCurrentProxy()), $player->getCurrentServerName() ?? $player->getCurrentProxyName())->call();

        $player->setCurrentServer(null);
        $player->setCurrentProxy(null);

        #PlayerSyncPacket::create($player, true)->broadcastPacket();
    }

    public function get(string $name): ?CloudPlayer {
        if (isset($this->players[$name])) return $this->players[$name];
        return array_find($this->players, fn($player) => $player->getXboxUserId() == $name ||
            $player->getUniqueId() == $name);

    }

    public function getAll(Template|CloudServer|ServerGroup|null $object = null): array {
        if ($object !== null) {
            $objectName = $object->getName();
            return array_filter($this->players, function (CloudPlayer $player) use ($objectName): bool {
                return $player->getCurrentServerName() == $objectName ||
                    $player->getCurrentProxyName() == $objectName ||
                    $player->getCurrentServer()?->getTemplateName() == $objectName ||
                    $player->getCurrentProxy()?->getTemplateName() == $objectName ||
                    $player->getCurrentServer()?->getTemplate()?->getParentServerGroup()?->getName() == $objectName ||
                    $player->getCurrentProxy()?->getTemplate()?->getParentServerGroup()?->getName() == $objectName;
            });
        }
        return $this->players;
    }
}