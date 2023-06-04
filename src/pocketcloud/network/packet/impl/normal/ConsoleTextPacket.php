<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\types\LogType;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\util\CloudLogger;

class ConsoleTextPacket extends CloudPacket {

    public function __construct(
        private string $text = "",
        private ?LogType $logType = null
    ) {
        if ($this->logType === null) $this->logType = LogType::INFO();
    }

    public function encodePayload(PacketData $packetData) {
        $packetData->write($this->text);
        $packetData->writeLogType($this->logType);
    }

    public function decodePayload(PacketData $packetData) {
        $this->text = $packetData->readString();
        $this->logType = $packetData->readLogType();
    }

    public function getText(): string {
        return $this->text;
    }

    public function getLogType(): ?LogType {
        return $this->logType;
    }

    public function handle(ServerClient $client) {
        if ($this->logType === LogType::INFO()) CloudLogger::get()->info($this->text);
        else if ($this->logType === LogType::DEBUG()) CloudLogger::get()->debug($this->text, true);
        else if ($this->logType === LogType::WARN()) CloudLogger::get()->warn($this->text);
        else if ($this->logType === LogType::ERROR()) CloudLogger::get()->error($this->text);
    }
}