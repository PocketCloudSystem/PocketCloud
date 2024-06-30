<?php

namespace pocketcloud\player;

use pocketcloud\event\impl\player\PlayerKickEvent;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\PlayerKickPacket;
use pocketcloud\network\packet\impl\normal\PlayerSyncPacket;
use pocketcloud\network\packet\impl\normal\PlayerTextPacket;
use pocketcloud\network\packet\impl\types\TextType;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateType;
use pocketcloud\util\ActionResult;
use pocketcloud\util\Utils;

class CloudPlayer {

    public function __construct(
        private readonly string $name,
        private readonly string $host,
        private readonly string $xboxUserId,
        private readonly string $uniqueId,
        private ?string $currentServer = null,
        private ?string $currentProxy = null
    ) {}

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
        return $this->currentServer === null ? null : CloudServerManager::getInstance()->getServerByName($this->currentServer);
    }

    public function getCurrentProxy(): ?CloudServer {
        return $this->currentProxy === null ? null : CloudServerManager::getInstance()->getServerByName($this->currentProxy);
    }

    public function getCurrentServerName(): ?string {
        return $this->currentServer;
    }

    public function getCurrentProxyName(): ?string {
        return $this->currentProxy;
    }

    public function setCurrentServer(?CloudServer $currentServer): void {
        $this->currentServer = $currentServer?->getName();
        if (CloudPlayerManager::getInstance()->getPlayerByName($this->name) !== null) Network::getInstance()->broadcastPacket(new PlayerSyncPacket($this));
    }

    public function setCurrentProxy(?CloudServer $currentProxy): void {
        $this->currentProxy = $currentProxy?->getName();
    }

    public function send(string $message, TextType $textType): ActionResult {
        return Network::getInstance()->broadcastPacket(new PlayerTextPacket($this->getName(), $message, $textType), ...ServerClientManager::getInstance()->pickClients(fn(ServerClient $client) => $client->getServer() !== null && $client->getServer()->getTemplate()->getTemplateType() === TemplateType::PROXY()))->isEveryActionSuccessful() ?
            ActionResult::success() : ActionResult::failure();
    }

    public function sendMessage(string $message): ActionResult {
        return $this->send($message, TextType::MESSAGE());
    }

    public function sendPopup(string $message): ActionResult {
        return $this->send($message, TextType::POPUP());
    }

    public function sendTip(string $message): ActionResult {
        return $this->send($message, TextType::TIP());
    }

    public function sendTitle(string $message): ActionResult {
        return $this->send($message, TextType::TITLE());
    }

    public function sendActionBarMessage(string $message): ActionResult {
        return $this->send($message, TextType::ACTION_BAR());
    }

    public function sendToastNotification(string $title, string $body): ActionResult {
        return $this->send($title . "\n" .  $body, TextType::TOAST_NOTIFICATION());
    }

    public function kick(string $reason = ""): ActionResult {
        ($ev = new PlayerKickEvent($this, $reason))->call();
        if ($ev->isCancelled()) return ActionResult::failure("Event cancelled");
        if ($this->getCurrentProxy() === null) return $this->getCurrentServer()?->sendPacket(new PlayerKickPacket($this->getName(), $reason));
        return $this->getCurrentProxy()->sendPacket(new PlayerKickPacket($this->getName(), $reason));
    }

    public function toArray(): array {
        return [
            "name" => $this->name,
            "host" => $this->host,
            "xboxUserId" => $this->xboxUserId,
            "uniqueId" => $this->uniqueId,
            "currentServer" => $this->getCurrentServerName(),
            "currentProxy" => $this->getCurrentProxyName()
        ];
    }

    public static function fromArray(array $player): ?self {
        if (!Utils::containKeys($player, "name", "host", "xboxUserId", "uniqueId")) return null;
        return new CloudPlayer(
            $player["name"],
            $player["host"],
            $player["xboxUserId"],
            $player["uniqueId"],
            (!isset($player["currentServer"]) ? null : $player["currentServer"]),
            (!isset($player["currentProxy"]) ? null : $player["currentProxy"])
        );
    }
}