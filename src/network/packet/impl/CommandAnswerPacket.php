<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CommandAnswerPacket extends CloudPacket implements CloudboundPacket {

    public function __construct(private ?ServerCommandExecutionResult $commandExecutionResult = null) {}

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->handleCommandResponse($this->commandExecutionResult);
        }
    }

    public function encodePayload(PacketData $packetData): void {}

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAllTypeSafe([&$this->commandExecutionResult], [fn() => $packetData->readServerCommandExecutionResult()]);
    }

    public function getCommandExecutionResult(): ?ServerCommandExecutionResult {
        return $this->commandExecutionResult;
    }

    public static function create(ServerCommandExecutionResult $commandExecutionResult): self {
        return new self($commandExecutionResult);
    }
}