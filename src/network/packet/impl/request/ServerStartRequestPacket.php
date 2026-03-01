<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\ServerErrorReason;
use pocketcloud\cloud\network\packet\impl\response\ServerStartResponsePacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

final class ServerStartRequestPacket extends RequestPacket {

    public function __construct(
        private string $template = "",
        private int $count = 0
    ) {}

    public function handle(ServerClient $client): void {
        if (($template = TemplateManager::getInstance()->get($this->template)) !== null) {
            if (count(CloudServerManager::getInstance()->getAll($template)) < $template->getSettings()->getMaxServerCount()) {
                CloudServerManager::getInstance()->start($template, $this->count);
                $this->sendResponse(ServerStartResponsePacket::create(ServerErrorReason::NONE), $client);
            } else $this->sendResponse(ServerStartResponsePacket::create(ServerErrorReason::MAX_SERVERS), $client);
        } else $this->sendResponse(ServerStartResponsePacket::create(ServerErrorReason::TEMPLATE_EXISTENCE), $client);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->template, $this->count);
    }

    public function getTemplate(): string {
        return $this->template;
    }

    public function getCount(): int {
        return $this->count;
    }

    public static function create(string $template, int $count): self {
        return new self($template, $count);
    }
}