<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\types\CommandExecutionResult;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\promise\Promise;

class CommandSendAnswerPacket extends CloudPacket {

    public function __construct(private ?CommandExecutionResult $result = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeCommandExecutionResult($this->result);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->result = $packetData->readCommandExecutionResult();
    }

    public function getResult(): ?CommandExecutionResult {
        return $this->result;
    }

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $promise = $server->getCloudServerStorage()->get("command_promise");
            if ($promise instanceof Promise) {
                $promise->resolve($this->result);
            }
        }
    }
}