<?php

namespace pocketcloud\network\packet\utils;

use JsonSerializable;
use pocketcloud\network\packet\impl\types\CommandExecutionResult;
use pocketcloud\network\packet\impl\types\DisconnectReason;
use pocketcloud\network\packet\impl\types\ErrorReason;
use pocketcloud\network\packet\impl\types\LogType;
use pocketcloud\network\packet\impl\types\TextType;
use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\player\CloudPlayer;
use pocketcloud\server\CloudServer;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\template\Template;

final class PacketData implements JsonSerializable {

    public function __construct(private array $data = []) {}

    public function write(mixed $v): self {
        $this->data[] = $v;
        return $this;
    }

    public function writeServer(CloudServer $server): self {
        return $this->write($server->toArray());
    }

    public function writeCommandExecutionResult(CommandExecutionResult $result): self {
        return $this->write($result->toArray());
    }

    public function writeLogType(LogType $logType): self {
        return $this->write($logType->getName());
    }

    public function writeServerStatus(ServerStatus $status): self {
        return $this->write($status->getName());
    }

    public function writeTemplate(Template $template): self {
        return $this->write($template->toArray());
    }

    public function writePlayer(CloudPlayer $player): self {
        return $this->write($player->toArray());
    }

    public function writeDisconnectReason(DisconnectReason $disconnectReason): self {
        return $this->write($disconnectReason->getName());
    }

    public function writeErrorReason(ErrorReason $errorReason): self {
        return $this->write($errorReason->getName());
    }

    public function writeVerifyStatus(VerifyStatus $verifyStatus): self {
        return $this->write($verifyStatus->getName());
    }

    public function writeTextType(TextType $textType): self {
        return $this->write($textType->getName());
    }

    public function read(): mixed {
        if (count($this->data) > 0) {
            $get = $this->data[0];
            unset($this->data[0]);
            $this->data = array_values($this->data);
            return $get;
        }
        return null;
    }

    public function readString(): ?string {
        $read = $this->read();
        if ($read === null) return null;
        return (string) $read;
    }

    public function readInt(): ?int {
        $read = $this->read();
        if ($read === null) return null;
        return intval($read);
    }

    public function readFloat(): ?float {
        $read = $this->read();
        if ($read === null) return null;
        return floatval($read);
    }

    public function readBool(): ?bool {
        $read = $this->read();
        if ($read === null) return null;
        return boolval($read);
    }

    public function readArray(): ?array {
        $read = $this->read();
        if ($read === null) return null;
        if (is_array($read)) return $read;
        return [];
    }

    public function readServer(): ?CloudServer {
        return CloudServer::fromArray($this->readArray());
    }

    public function readCommandExecutionResult(): ?CommandExecutionResult {
        return CommandExecutionResult::fromArray($this->readArray());
    }

    public function readLogType(): ?LogType {
        return LogType::getTypeByName($this->readString());
    }

    public function readServerStatus(): ?ServerStatus {
        return ServerStatus::getServerStatusByName($this->readString());
    }

    public function readTemplate(): ?Template {
        return Template::fromArray($this->readArray());
    }

    public function readPlayer(): ?CloudPlayer {
        return CloudPlayer::fromArray($this->readArray());
    }

    public function readDisconnectReason(): ?DisconnectReason {
        return DisconnectReason::getReasonByName($this->readString());
    }

    public function readErrorReason(): ?ErrorReason {
        return ErrorReason::getReasonByName($this->readString());
    }

    public function readVerifyStatus(): ?VerifyStatus {
        return VerifyStatus::getStatusByName($this->readString());
    }

    public function readTextType(): ?TextType {
        return TextType::getTypeByName($this->readString());
    }

    public function jsonSerialize(): array {
        return $this->data;
    }
}