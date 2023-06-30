<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\config\NotifyList;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\impl\response\CheckPlayerNotifyResponsePacket;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\network\packet\RequestPacket;

class CheckPlayerNotifyRequestPacket extends RequestPacket {

    public function __construct(
        string $requestId = "",
        private string $player = ""
    ) {
        parent::__construct($requestId);
    }

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
        $this->sendResponse(new CheckPlayerNotifyResponsePacket(NotifyList::is($this->player)), $client);
    }
}