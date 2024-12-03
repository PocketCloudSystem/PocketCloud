<?php

namespace pocketcloud\cloud\player;

use pocketcloud\cloud\event\impl\player\PlayerKickEvent;
use pocketcloud\cloud\network\packet\impl\type\TextType;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\Utils;

final class CloudPlayer {

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
        return $this->currentServer === null ? null : CloudServerManager::getInstance()->get($this->currentServer);
    }

    public function getCurrentProxy(): ?CloudServer {
        return $this->currentProxy === null ? null : CloudServerManager::getInstance()->get($this->currentProxy);
    }

    public function getCurrentServerName(): ?string {
        return $this->currentServer;
    }

    public function getCurrentProxyName(): ?string {
        return $this->currentProxy;
    }

    public function setCurrentServer(?CloudServer $currentServer): void {
        CloudLogger::get()->debug("Changing current server of " . $this->name . " to " . ($currentServer?->getName() ?? "NULL"));
        $this->currentServer = $currentServer?->getName();
        //todo send sync packet
    }

    public function setCurrentProxy(?CloudServer $currentProxy): void {
        CloudLogger::get()->debug("Changing current proxy of " . $this->name . " to " . ($currentProxy?->getName() ?? "NULL"));
        $this->currentProxy = $currentProxy?->getName();
    }

    public function send(string $message, TextType $textType): void {
        CloudLogger::get()->debug("Sending text (" . $textType->getName() . ") to  " . $this->name);
        //todo send text packet
    }

    public function sendMessage(string $message): void {
        $this->send($message, TextType::MESSAGE());
    }

    public function sendPopup(string $message): void {
        $this->send($message, TextType::POPUP());
    }

    public function sendTip(string $message): void {
        $this->send($message, TextType::TIP());
    }

    public function sendTitle(string $message): void {
        $this->send($message, TextType::TITLE());
    }

    public function sendActionBarMessage(string $message): void {
        $this->send($message, TextType::ACTION_BAR());
    }

    public function sendToastNotification(string $title, string $body): void {
        $this->send($title . "\n" .  $body, TextType::TOAST_NOTIFICATION());
    }

    public function kick(string $reason = ""): void {
        CloudLogger::get()->debug("Kicking " . $this->name . " from the network, reason: " . ($reason == "" ? "NULL" : $reason));
        ($ev = new PlayerKickEvent($this, $reason))->call();
        if ($ev->isCancelled()) return;
        //todo send kick packet
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