<?php

namespace pocketcloud\cloud\network\packet\impl\request\client;

use pocketcloud\cloud\network\packet\RequestClientPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CommandExecuteRequestPacket extends RequestClientPacket {

    public function __construct(
        private readonly string $commandLine = "",
        private readonly string $id = ""
    ) {}

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