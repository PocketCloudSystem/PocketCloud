<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\response\CloudServerStartResponsePacket;
use pocketcloud\cloud\network\packet\impl\type\ErrorReason;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

class CloudServerStartRequestPacket extends RequestPacket {

    public function __construct(
        private string $template = "",
        private int $count = 0
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->template);
        $packetData->write($this->count);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->template = $packetData->readString();
        $this->count = $packetData->readInt();
    }

    public function getTemplate(): string {
        return $this->template;
    }

    public function getCount(): int {
        return $this->count;
    }

    public function handle(ServerClient $client): void {
        if (($template = TemplateManager::getInstance()->get($this->template)) !== null) {
            if (count(CloudServerManager::getInstance()->getAllByTemplate($template)) < $template->getSettings()->getMaxServerCount()) {
                CloudServerManager::getInstance()->start($template, $this->count);
                $this->sendResponse(new CloudServerStartResponsePacket(ErrorReason::NO_ERROR()), $client);
            } else $this->sendResponse(new CloudServerStartResponsePacket(ErrorReason::MAX_SERVERS()), $client);
        } else $this->sendResponse(new CloudServerStartResponsePacket(ErrorReason::TEMPLATE_EXISTENCE()), $client);
    }
}