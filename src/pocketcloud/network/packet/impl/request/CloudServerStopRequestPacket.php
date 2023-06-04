<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\impl\response\CloudServerStopResponsePacket;
use pocketcloud\network\packet\impl\types\ErrorReason;
use pocketcloud\network\packet\RequestPacket;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;
use pocketcloud\network\packet\utils\PacketData;

class CloudServerStopRequestPacket extends RequestPacket {

    public function __construct(
        string $requestId = "",
        private string $server = ""
    ) {
        parent::__construct($requestId);
    }

    public function encodePayload(PacketData $packetData) {
        $packetData->write($this->server);
    }

    public function decodePayload(PacketData $packetData) {
        $this->server = $packetData->readString();
    }

    public function getServer(): string {
        return $this->server;
    }

    public function handle(ServerClient $client) {
        if (($server = CloudServerManager::getInstance()->getServerByName($this->server)) !== null) {
            CloudServerManager::getInstance()->stopServer($server);
            $this->sendResponse(new CloudServerStopResponsePacket(ErrorReason::NO_ERROR()), $client);
        } else if (($template = TemplateManager::getInstance()->getTemplateByName($this->server)) !== null) {
            CloudServerManager::getInstance()->stopTemplate($template);
            $this->sendResponse(new CloudServerStopResponsePacket(ErrorReason::NO_ERROR()), $client);
        } else $this->sendResponse(new CloudServerStopResponsePacket(ErrorReason::SERVER_EXISTENCE()), $client);
    }
}