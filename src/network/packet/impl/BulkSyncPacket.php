<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;

final class BulkSyncPacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly ?array $servers = [],
        private readonly ?array $templates = [],
        private readonly ?array $players = [],
        private readonly ?array $groups = []
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $servers = [];
        foreach ($this->servers as $server) {
            if ($server instanceof CloudServer) {
                $servers[] = $server->write();
            }
        }

        $templates = [];
        foreach ($this->templates as $template) {
            if ($template instanceof Template) {
                $templates[] = $template->write();
            }
        }

        $players = [];
        foreach ($this->players as $player) {
            if ($player instanceof CloudPlayer) {
                $players[] = $player->write();
            }
        }

        $groups = [];
        foreach ($this->groups as $group) {
            if ($group instanceof ServerGroup) {
                $groups[] = $group->write();
            }
        }

        $packetData->writeAll($servers, $templates, $players, $groups);
    }

    public function decodePayload(PacketData $packetData): void {}

    public static function create(array $servers, array $templates, array $players, array $groups): self {
        return new self($servers, $templates, $players, $groups);
    }

    public static function generate(): self {
        return self::create(
            CloudServerManager::getInstance()->getAll(),
            TemplateManager::getInstance()->getAll(),
            CloudPlayerManager::getInstance()->getAll(),
            ServerGroupManager::getInstance()->getAll()
        );
    }
}