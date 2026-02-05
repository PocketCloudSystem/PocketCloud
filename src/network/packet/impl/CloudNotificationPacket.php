<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\NotificationType;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayerManager;

final class CloudNotificationPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function __construct(
        private ?NotificationType $notificationType = null,
        private array $args = []
    ) {}

    public function handle(ServerClient $client): void {
        $extraArgs = [];
        if ($this->notificationType->canLog()) {
            switch ($this->notificationType) {
                case NotificationType::PLAYER_JOIN_FAILED: {
                    [$player, $server, $reason] = [$this->args["player"], $this->args["server"], $this->args["reason"]];
                    $alreadyOnAServer = CloudPlayerManager::getInstance()->get($player)?->getCurrentServerName() !== null;
                    CloudLogger::get()->info("The player §b{} §rtried to join" . ($alreadyOnAServer ? "" : " via") . " §b{}§r, but got §ckicked§r: §b{}", $player, $server, $this->formatReason($reason));
                    break;
                }
                case NotificationType::PLAYER_KICKED: {
                    [$player, $server, $reason] = [$this->args["player"], $this->args["server"], $this->args["reason"]];
                    CloudLogger::get()->info("The player §b{} §rhas been §ckicked §rfrom §b{}§r: §b{}", $player, $server, $this->formatReason($reason));
                    break;
                }
                default: break;
            }
        }

        $this->notificationType->notify($this->args);
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->notificationType, $this->args);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAllTypeSafe([&$this->notificationType, &$this->args], [fn() => $packetData->readNotificationType(), fn() => $packetData->readArray()]);
    }

    public function getNotificationType(): ?NotificationType {
        return $this->notificationType;
    }

    public function getArgs(): array {
        return $this->args;
    }

    private function formatReason(string $reason): string {
        $newReason = substr(current(explode("\n", $reason)), 0, 100);
        if (strlen($newReason) !== strlen($reason)) $newReason .= "...";
        return $newReason;
    }

    public static function create(NotificationType $notificationType, array $args): self {
        return new self($notificationType, $args);
    }
}