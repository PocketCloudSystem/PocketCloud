<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\config\impl\MaintenanceList;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\impl\response\CheckPlayerMaintenanceResponsePacket;
use pocketcloud\network\packet\RequestPacket;
use pocketcloud\network\packet\utils\PacketData;

class CheckPlayerMaintenanceRequestPacket extends RequestPacket {

    public function __construct(private string $player = "") {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->player);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->player = $packetData->readString();
    }

    public function getPlayer(): string {
        return $this->player;
    }

    public function handle(ServerClient $client): void {
        $this->sendResponse(new CheckPlayerMaintenanceResponsePacket(MaintenanceList::is($this->player)), $client);
    }
}