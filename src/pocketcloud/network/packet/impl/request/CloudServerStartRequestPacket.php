<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\impl\response\CloudServerStartResponsePacket;
use pocketcloud\network\packet\impl\types\ErrorReason;
use pocketcloud\network\packet\RequestPacket;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;
use pocketcloud\network\packet\utils\PacketData;

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
        if (($template = TemplateManager::getInstance()->getTemplateByName($this->template)) !== null) {
            if (count(CloudServerManager::getInstance()->getServersByTemplate($template)) < $template->getSettings()->getMaxServerCount()) {
                CloudServerManager::getInstance()->startServer($template, $this->count);
                $this->sendResponse(new CloudServerStartResponsePacket(ErrorReason::NO_ERROR()), $client);
            } else $this->sendResponse(new CloudServerStartResponsePacket(ErrorReason::MAX_SERVERS()), $client);
        } else $this->sendResponse(new CloudServerStartResponsePacket(ErrorReason::TEMPLATE_EXISTENCE()), $client);
    }
}