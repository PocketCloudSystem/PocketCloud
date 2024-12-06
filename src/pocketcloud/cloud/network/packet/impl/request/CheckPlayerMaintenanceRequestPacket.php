<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\cache\MaintenanceList;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\response\CheckPlayerMaintenanceResponsePacket;
use pocketcloud\cloud\network\packet\RequestPacket;

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