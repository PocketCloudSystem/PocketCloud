<?php

namespace pocketcloud\cloud\network\packet\util;

use JsonSerializable;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\network\packet\data\LogType;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\network\packet\data\ServerDisconnectReason;
use pocketcloud\cloud\network\packet\data\ServerErrorReason;
use pocketcloud\cloud\network\packet\data\TextType;
use pocketcloud\cloud\network\packet\data\VerifyStatus;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\template\Template;

final class PacketData implements JsonSerializable {

    public function __construct(private array $data = []) {}

    public function write(mixed $v): self {
        $this->data[] = $v;
        return $this;
    }

    public function writeTemplate(Template $template): self {
        return $this->write($template->write());
    }

    public function writeServer(CloudServer $server): self {
        return $this->write($server->write());
    }

    public function writeServerGroup(ServerGroup $serverGroup): self {
        return $this->write($serverGroup->write());
    }

    public function writePlayer(CloudPlayer $player): self {
        return $this->write($player->write());
    }

    public function writeServerCommandExecutionResult(ServerCommandExecutionResult $result): self {
        return $this->write($result->write());
    }

    public function writeLogType(LogType $logType): self {
        return $this->write($logType->getName());
    }

    public function writeServerStatus(ServerStatus $status): self {
        return $this->write($status->getName());
    }

    public function writeServerDisconnectReason(ServerDisconnectReason $serverDisconnectReason): self {
        return $this->write($serverDisconnectReason->getName());
    }

    public function writeServerErrorReason(ServerErrorReason $serverErrorReason): self {
        return $this->write($serverErrorReason->getName());
    }

    public function writeVerifyStatus(VerifyStatus $verifyStatus): self {
        return $this->write($verifyStatus->getName());
    }

    public function writeTextType(TextType $textType): self {
        return $this->write($textType->getName());
    }

    public function read(): mixed {
        return array_shift($this->data);
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
        if (is_array($read)) return $read;
        return null;
    }

    public function readTemplate(): ?Template {
        return Template::read($this->readArray());
    }

    public function readServer(): ?CloudServer {
        return CloudServer::read($this->readArray());
    }

    public function readServerGroup(): ?CloudServer {
        return CloudServer::read($this->readArray());
    }

    public function readPlayer(): ?CloudPlayer {
        return CloudPlayer::read($this->readArray());
    }

    public function readServerCommandExecutionResult(): ?ServerCommandExecutionResult {
        return ServerCommandExecutionResult::read($this->readArray());
    }

    public function readLogType(): ?LogType {
        return LogType::fromName($this->readString());
    }

    public function readServerStatus(): ?ServerStatus {
        return ServerStatus::fromName($this->readString());
    }

    public function readServerDisconnectReason(): ?ServerDisconnectReason {
        return ServerDisconnectReason::fromName($this->readString());
    }

    public function readServerErrorReason(): ?ServerErrorReason {
        return ServerErrorReason::fromName($this->readString());
    }

    public function readVerifyStatus(): ?VerifyStatus {
        return VerifyStatus::fromName($this->readString());
    }

    public function readTextType(): ?TextType {
        return TextType::fromName($this->readString());
    }

    public function isEmpty(): bool {
        return empty($this->data);
    }

    public function count(): int {
        return count($this->data);
    }

    public function jsonSerialize(): array {
        return $this->data;
    }
}