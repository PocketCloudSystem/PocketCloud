<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\response\CloudServerStopResponsePacket;
use pocketcloud\cloud\network\packet\impl\type\ErrorReason;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\server\CloudServerManager;

class CloudServerStopRequestPacket extends RequestPacket {

    public function __construct(private string $server = "") {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->server);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->server = $packetData->readString();
    }

    public function getServer(): string {
        return $this->server;
    }

    public function handle(ServerClient $client): void {
        if (CloudServerManager::getInstance()->stop($this->server)) {
            $this->sendResponse(new CloudServerStopResponsePacket(ErrorReason::NO_ERROR()), $client);
        } else $this->sendResponse(new CloudServerStopResponsePacket(ErrorReason::SERVER_EXISTENCE()), $client);
    }
}