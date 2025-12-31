<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CommandExecutePacket extends CloudPacket implements ClientboundPacket {

    public function __construct(private readonly string $commandLine = "") {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->commandLine);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->commandLine);
    }

    public function getCommandLine(): string {
        return $this->commandLine;
    }

    public static function create(string $commandLine): self {
        return new self($commandLine);
    }
}