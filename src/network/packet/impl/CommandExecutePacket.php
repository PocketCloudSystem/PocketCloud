<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CommandExecutePacket extends CloudPacket implements ClientboundPacket {

    public function __construct(
        private readonly string $commandLine = "",
        private readonly string $id = ""
    ) {}

    public function handle(ServerClient $client): void {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->commandLine, $this->id);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function getCommandLine(): string {
        return $this->commandLine;
    }

    public function getId(): string {
        return $this->id;
    }

    public static function create(string $commandLine, string $id): self {
        return new self($commandLine, $id);
    }
}