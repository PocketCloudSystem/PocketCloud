<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\response\CheckPlayerNotifyResponsePacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\provider\CloudProvider;

final class CheckPlayerNotifyRequestPacket extends RequestPacket {

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
        CloudProvider::current()->hasNotificationsEnabled($this->player)
            ->then(fn(bool $v) => $this->sendResponse(new CheckPlayerNotifyResponsePacket($v), $client));
    }
}