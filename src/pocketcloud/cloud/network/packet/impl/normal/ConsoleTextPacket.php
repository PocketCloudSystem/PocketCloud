<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\type\LogType;
use pocketcloud\cloud\terminal\log\CloudLogger;

final class ConsoleTextPacket extends CloudPacket {

    public function __construct(
        private string $text = "",
        private ?LogType $logType = null
    ) {
        if ($this->logType === null) $this->logType = LogType::INFO();
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->text);
        $packetData->writeLogType($this->logType);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->text = $packetData->readString();
        $this->logType = $packetData->readLogType();
    }

    public function getText(): string {
        return $this->text;
    }

    public function getLogType(): ?LogType {
        return $this->logType;
    }

    public function handle(ServerClient $client): void {
        if ($this->logType === LogType::INFO()) CloudLogger::get()->info($this->text);
        else if ($this->logType === LogType::DEBUG()) CloudLogger::get()->debug($this->text, true);
        else if ($this->logType === LogType::WARN()) CloudLogger::get()->warn($this->text);
        else if ($this->logType === LogType::ERROR()) CloudLogger::get()->error($this->text);
        else if ($this->logType === LogType::SUCCESS()) CloudLogger::get()->success($this->text);
    }

    public static function create(string $text = "", ?LogType $logType = null): self {
        return new self($text, $logType);
    }
}