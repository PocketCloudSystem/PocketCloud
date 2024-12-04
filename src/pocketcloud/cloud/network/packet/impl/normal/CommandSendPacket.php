<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;

class CommandSendPacket extends CloudPacket {

    public function __construct(private string $commandLine = "") {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->commandLine);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->commandLine = $packetData->readString();
    }

    public function getCommandLine(): string {
        return $this->commandLine;
    }

    public function handle(ServerClient $client): void {}

    public static function create(string $commandLine): self {
        return new self($commandLine);
    }
}