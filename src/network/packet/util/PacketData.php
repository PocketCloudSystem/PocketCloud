<?php

namespace pocketcloud\cloud\network\packet\util;

use Closure;
use JsonSerializable;
use OutOfBoundsException;
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

    public function writeAll(mixed ...$v): void {
        foreach ($v as $item) {
            if ($item instanceof Template) $this->writeTemplate($item);
            else if ($item instanceof CloudServer) $this->writeServer($item);
            else if ($item instanceof ServerGroup) $this->writeServerGroup($item);
            else if ($item instanceof CloudPlayer) $this->writePlayer($item);
            else if ($item instanceof ServerCommandExecutionResult) $this->writeServerCommandExecutionResult($item);
            else if ($item instanceof LogType) $this->writeLogType($item);
            else if ($item instanceof ServerStatus) $this->writeServerStatus($item);
            else if ($item instanceof ServerDisconnectReason) $this->writeServerDisconnectReason($item);
            else if ($item instanceof ServerErrorReason) $this->writeServerErrorReason($item);
            else if ($item instanceof VerifyStatus) $this->writeVerifyStatus($item);
            else if ($item instanceof TextType) $this->writeTextType($item);
            else $this->write($item);
        }
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

    public function readAll(mixed &...$v): void {
        foreach ($v as &$item) {
            if ($this->isEmpty()) throw new OutOfBoundsException("Passed too many references, packet buffer is empty");
            $item = $this->read();
        }
    }

    /**
     * @param array $refs
     * @param array<Closure(PacketData $buffer): mixed> $readers
     * @return void
     */
    public function readAllTypeSafe(array $refs, array $readers = []): void {
        foreach ($refs as $i => &$item) {
            if ($this->isEmpty()) throw new OutOfBoundsException("Passed too many references, packet buffer is empty");
            $reader = $readers[$i] ?? fn(PacketData $buffer) => $buffer->read();
            $item = $reader($this);
        }
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