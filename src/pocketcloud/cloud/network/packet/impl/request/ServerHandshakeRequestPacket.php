<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\normal\KeepAlivePacket;
use pocketcloud\cloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
use pocketcloud\cloud\network\packet\impl\type\VerifyStatus;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\terminal\log\CloudLogger;

final class ServerHandshakeRequestPacket extends RequestPacket {

    public function __construct(
        private ?string $serverName = null,
        private ?int $processId = null,
        private ?int $maxPlayers = null
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->serverName)
            ->write($this->processId)
            ->write($this->maxPlayers);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->serverName = $packetData->readString();
        $this->processId = $packetData->readInt();
        $this->maxPlayers = $packetData->readInt();
    }

    public function getServerName(): ?string {
        return $this->serverName;
    }

    public function getProcessId(): ?int {
        return $this->processId;
    }

    public function getMaxPlayers(): ?int {
        return $this->maxPlayers;
    }

    public function handle(ServerClient $client): void {
        if (($server = CloudServerManager::getInstance()->get($this->serverName)) !== null) {
            ServerClientCache::getInstance()->add($server, $client);
            CloudLogger::get()->success("The server §b" . $server->getName() . " §rhas §aconnected §rto the cloud.");
            $server->getCloudServerData()->setMaxPlayers($this->maxPlayers);
            $server->getCloudServerData()->setProcessId($this->processId);
            $server->setVerifyStatus(VerifyStatus::VERIFIED());
            $this->sendResponse(new ServerHandshakeResponsePacket(VerifyStatus::VERIFIED()), $client);
            Network::getInstance()->broadcastPacket(new ServerSyncPacket($server), $client);
            $server->sync();
            $server->setServerStatus(ServerStatus::ONLINE());
            $server->sendPacket(new KeepAlivePacket());
        } else $this->sendResponse(new ServerHandshakeResponsePacket(VerifyStatus::DENIED()), $client);
    }
}