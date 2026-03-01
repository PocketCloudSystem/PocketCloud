<?php

namespace pocketcloud\cloud\network\packet\impl\response\client;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\network\packet\ResponseClientPacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class CommandExecuteResponsePacket extends ResponseClientPacket {

    public function __construct(private ?ServerCommandExecutionResult $commandExecutionResult = null) {}

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->handleCommandResponse($this->commandExecutionResult);
        }
    }

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