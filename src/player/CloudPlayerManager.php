<?php

namespace pocketcloud\cloud\player;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\player\PlayerConnectEvent;
use pocketcloud\cloud\event\impl\player\PlayerDisconnectEvent;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\network\packet\data\NotificationType;
use pocketcloud\cloud\network\packet\impl\PlayerSyncPacket;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class CloudPlayerManager {
    use SingletonTrait;

    /** @var array<CloudPlayer> */
    private array $players = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function add(CloudPlayer $player): void {
        $anyProxies = count(CloudServerManager::getInstance()->getAll(...TemplateType::onlyProxy())) > 0;
        if ($anyProxies && $player->getCurrentProxy() === null) {
            // This is used to prevent players to join via sub-servers instead of the main proxy.
            $player->kick("Joined via sub-server instead of a proxy.", "Please do not join via sub-servers.");
            return;
        }

        if (NotificationType::PLAYER_JOINED->canLog()) CloudLogger::get()->info("Player §b{} §rhas §aconnected §rvia §b{}§r.", $player->getName(), $player->getCurrentProxyName() ?? $player->getCurrentServerName());
        $this->players[$player->getName()] = $player;
        PlayerSyncPacket::create($player, false)->broadcastPacket();
        NotificationType::PLAYER_JOINED->notify(["player" => $player->getName(), "server" => $player->getCurrentServerName() ?? $player->getCurrentProxyName()]);

        new PlayerConnectEvent($player, ($player->getCurrentServer() ?? $player->getCurrentProxy()))->call();
    }

    public function remove(CloudPlayer $player): void {
        if (NotificationType::PLAYER_JOINED->canLog()) CloudLogger::get()->info("Player §b{} §cdisconnected §rfrom §b{}§r.", $player->getName(), $player->getCurrentServerName() ?? $player->getCurrentProxyName());
        if (isset($this->players[$player->getName()])) unset($this->players[$player->getName()]);
        NotificationType::PLAYER_LEFT->notify(["player" => $player->getName(), "server" => $player->getCurrentServerName() ?? $player->getCurrentProxyName()]);

        new PlayerDisconnectEvent($player, ($player->getCurrentServer() ?? $player->getCurrentProxy()), $player->getCurrentServerName() ?? $player->getCurrentProxyName())->call();

        $player->setCurrentServer(null);
        $player->setCurrentProxy(null);
        PlayerSyncPacket::create($player, true)->broadcastPacket();
    }

    public function get(string $name): ?CloudPlayer {
        if (isset($this->players[$name])) return $this->players[$name];
        return array_find($this->players, fn($player) => $player->getXboxUserId() == $name || $player->getUniqueId() == $name);
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