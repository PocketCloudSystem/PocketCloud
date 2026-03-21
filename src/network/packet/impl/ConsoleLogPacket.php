<?php

namespace pocketcloud\cloud\network\packet\impl;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\LogType;
use pocketcloud\cloud\network\packet\util\PacketData;

final class ConsoleLogPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket {

    public function __construct(
        private string $message = "",
        private ?LogType $logType = null
    ) {}

    public function handle(ServerClient $client): void {
        CloudLogger::get()->log($this->logType->toLogLevel(), $this->message);
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->message, $this->logType);
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAllTypeSafe([&$this->message, &$this->logType], [fn() => $packetData->readString(), fn() => $packetData->readLogType()]);
    }

    public function getMessage(): string {
        return $this->message;
    }

    public function getLogType(): ?LogType {
        return $this->logType;
    }

    public static function create(string $message, LogType $logType): self {
        return new self($message, $logType);
    }
}