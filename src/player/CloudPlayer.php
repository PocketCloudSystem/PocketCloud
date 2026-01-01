<?php

namespace pocketcloud\cloud\player;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\player\PlayerKickEvent;
use pocketcloud\cloud\network\packet\data\TextType;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\util\Utils;

final class CloudPlayer {

    public function __construct(
        private readonly string $name,
        private readonly string $address,
        private readonly string $xboxUserId,
        private readonly string $uniqueId,
        private ?string $currentServer = null,
        private ?string $currentProxy = null
    ) {}

    public function setCurrentServer(?CloudServer $currentServer): void {
        CloudLogger::get()->debug("Changing current server of " . $this->name . " to " . ($currentServer?->getName() ?? "NULL"));
        $this->currentServer = $currentServer?->getName();
        //TODO: PlayerSyncPacket::create($this, false)->broadcastPacket();
    }

    public function setCurrentProxy(?CloudServer $currentProxy): void {
        CloudLogger::get()->debug("Changing current proxy of " . $this->name . " to " . ($currentProxy?->getName() ?? "NULL"));
        $this->currentProxy = $currentProxy?->getName();
    }

    public function kick(string $reason = ""): void {
        CloudLogger::get()->info("Kicking {}, reason: {}", $this->name, ($reason == "" ? "NULL" : $reason));
        ($ev = new PlayerKickEvent($this, $reason))->call();
        if ($ev->isCancelled()) return;
        #TODO: PlayerKickPacket::create($this->getName(), $reason)->sendPacket($this->getCurrentProxy() ?? $this->getCurrentServer());
    }

    public function send(string $message, TextType $textType): void {
        #CloudLogger::get()->debug("Sending text (" . $textType->getName() . ") to  " . $this->name);
        /** PlayerTextPacket::create($this->getName(), $message, $textType)->broadcastPacket(
            ...ServerClientCache::getInstance()->pick(fn(ServerClient $client) => $client->getServer() !== null && $client->getServer()->getTemplate()->getTemplateType()->isProxy())
        ); */
    }

    public function sendMessage(string $message): void {
        $this->send($message, TextType::MESSAGE);
    }

    public function sendPopup(string $message): void {
        $this->send($message, TextType::POPUP);
    }

    public function sendTip(string $message): void {
        $this->send($message, TextType::TIP);
    }

    public function sendTitle(string $message): void {
        $this->send($message, TextType::TITLE);
    }

    public function sendActionBarMessage(string $message): void {
        $this->send($message, TextType::ACTION_BAR);
    }

    public function sendToastNotification(string $title, string $body): void {
        $this->send($title . "\n" .  $body, TextType::TOAST_NOTIFICATION);
    }

    public function getName(): string {
        return $this->name;
    }

    public function getAddress(): string {
        return $this->address;
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

    public function write(): array {
        return [
            "name" => $this->name,
            "address" => $this->address,
            "xboxUserId" => $this->xboxUserId,
            "uniqueId" => $this->uniqueId,
            "currentServer" => $this->currentServer,
            "currentProxy" => $this->currentProxy
        ];
    }

    public static function read(array $data): ?self {
        if (!Utils::containKeys($data, "name", "address", "xboxUserId", "uniqueId")) return null;
        return new CloudPlayer(
            $data["name"],
            $data["address"],
            $data["xboxUserId"],
            $data["uniqueId"],
            $data["currentServer"] ?? null,
            $data["currentProxy"] ?? null
        );
    }
}