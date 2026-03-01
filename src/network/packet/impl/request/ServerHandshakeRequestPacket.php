<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\data\VerifyStatus;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
use pocketcloud\cloud\network\packet\impl\ServerSyncPacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\util\ServerStatus;

final class ServerHandshakeRequestPacket extends RequestPacket {

    public function __construct(
        private string $serverName = "",
        private int $processId = -1,
        private int $maxPlayers = -1,
    ) {}

    public function handle(ServerClient $client): void {
        if (($server = CloudServerManager::getInstance()->get($this->serverName)) !== null && ServerClientCache::getInstance()->getServer($client) === null) {
            ServerClientCache::getInstance()->add($server, $client);
            CloudLogger::get()->success("The server §b{} §rhas §aconnected §rto the cloud.", $server->getName());
            $server->getServerData()->setMaxPlayers($this->maxPlayers);
            $server->getServerData()->setProcessId($this->processId);
            $server->setVerifyStatus(VerifyStatus::VERIFIED);
            $server->addToProxies();
            $server->sync();
            $this->sendResponse(new ServerHandshakeResponsePacket(VerifyStatus::VERIFIED), $client);
            ServerSyncPacket::create($server, false)->broadcastPacket()->failure(fn(?string $reason) => CloudLogger::get()->warn("Failed to broadcast server creation, reason: §b{}", $reason ?? "None"));
            $server->setServerStatus(ServerStatus::ONLINE);
        } else {
            CloudLogger::get()->warn("Denied server handshake request from §b{} §8(§b{}§8)§r, duplicate server...", $this->serverName, $client->getAddress());
            $this->sendResponse(new ServerHandshakeResponsePacket(VerifyStatus::DENIED), $client);
        }
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->serverName, $this->processId, $this->maxPlayers);
    }

    public static function create(string $serverName, int $processId, int $maxPlayers): self {
        return new self($serverName, $processId, $maxPlayers);
    }
}