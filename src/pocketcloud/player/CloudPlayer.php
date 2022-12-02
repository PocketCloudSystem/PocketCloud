<?php

namespace pocketcloud\player;

use pocketcloud\event\impl\player\PlayerKickEvent;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\LocalPlayerUpdatePacket;
use pocketcloud\network\packet\impl\normal\PlayerKickPacket;
use pocketcloud\network\packet\impl\normal\PlayerTextPacket;
use pocketcloud\network\packet\impl\types\TextType;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\utils\Utils;

class CloudPlayer {

    public function __construct(private string $name, private string $host, private string $xboxUserId, private string $uniqueId, private ?CloudServer $currentServer = null, private ?CloudServer $currentProxy = null) {}

    public function getName(): string {
        return $this->name;
    }

    public function getHost(): string {
        return $this->host;
    }

    public function getXboxUserId(): string {
        return $this->xboxUserId;
    }

    public function getUniqueId(): string {
        return $this->uniqueId;
    }

    public function getCurrentServer(): ?CloudServer {
        return $this->currentServer;
    }

    public function getCurrentProxy(): ?CloudServer {
        return $this->currentProxy;
    }

    public function setCurrentServer(?CloudServer $currentServer): void {
        $this->currentServer = $currentServer;
        if (CloudPlayerManager::getInstance()->getPlayerByName($this->name) !== null) Network::getInstance()->broadcastPacket(new LocalPlayerUpdatePacket($this->getName(), $currentServer?->getName()));
    }

    public function setCurrentProxy(?CloudServer $currentProxy): void {
        $this->currentProxy = $currentProxy;
    }

    public function sendMessage(string $message) {
        Network::getInstance()->broadcastPacket(new PlayerTextPacket($this->getName(), $message, TextType::MESSAGE()));
    }

    public function sendPopup(string $message) {
        Network::getInstance()->broadcastPacket(new PlayerTextPacket($this->getName(), $message, TextType::POPUP()));
    }

    public function sendTip(string $message) {
        Network::getInstance()->broadcastPacket(new PlayerTextPacket($this->getName(), $message, TextType::TIP()));
    }

    public function sendTitle(string $message) {
        Network::getInstance()->broadcastPacket(new PlayerTextPacket($this->getName(), $message, TextType::TITLE()));
    }

    public function sendActionBarMessage(string $message) {
        Network::getInstance()->broadcastPacket(new PlayerTextPacket($this->getName(), $message, TextType::ACTION_BAR()));
    }

    public function kick(string $reason = "") {
        ($ev = new PlayerKickEvent($this, $reason))->call();
        if ($ev->isCancelled()) return;
        if ($this->getCurrentProxy() === null) $this->getCurrentServer()?->sendPacket(new PlayerKickPacket($this->getName(), $reason));
        else $this->getCurrentProxy()->sendPacket(new PlayerKickPacket($this->getName(), $reason));
    }

    public function toArray(): array {
        return [
            "name" => $this->name,
            "host" => $this->host,
            "xboxUserId" => $this->xboxUserId,
            "uniqueId" => $this->uniqueId,
            "currentServer" => $this->getCurrentServer()?->getName(),
            "currentProxy" => $this->getCurrentProxy()?->getName()
        ];
    }

    public static function fromArray(array $player): ?CloudPlayer {
        if (!Utils::containKeys($player, "name", "host", "xboxUserId", "uniqueId")) return null;
        return new CloudPlayer(
            $player["name"],
            $player["host"],
            $player["xboxUserId"],
            $player["uniqueId"],
            (!isset($player["currentServer"]) ? null : CloudServerManager::getInstance()->getServerByName($player["currentServer"])),
            (!isset($player["currentProxy"]) ? null : CloudServerManager::getInstance()->getServerByName($player["currentProxy"]))
        );
    }
}