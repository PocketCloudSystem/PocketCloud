<?php

namespace pocketcloud\network\packet\impl\request;

use pocketcloud\language\Language;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\KeepAlivePacket;
use pocketcloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\network\packet\impl\response\LoginResponsePacket;
use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\network\packet\RequestPacket;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\util\CloudLogger;
use pocketcloud\network\packet\utils\PacketData;

class LoginRequestPacket extends RequestPacket {

    public function __construct(
        private string $serverName = "",
        private int $processId = 0,
        private int $maxPlayers = 0
    ) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->serverName);
        $packetData->write($this->processId);
        $packetData->write($this->maxPlayers);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->serverName = $packetData->readString();
        $this->processId = $packetData->readInt();
        $this->maxPlayers = $packetData->readInt();
    }

    public function handle(ServerClient $client): void {
        if (($server = CloudServerManager::getInstance()->getServerByName($this->serverName)) !== null) {
            ServerClientManager::getInstance()->addClient($server, $client);
            CloudLogger::get()->info(Language::current()->translate("server.started", $server->getName()));
            $server->getCloudServerData()->setMaxPlayers($this->maxPlayers);
            $server->getCloudServerData()->setProcessId($this->processId);
            $server->setVerifyStatus(VerifyStatus::VERIFIED());
            $this->sendResponse(new LoginResponsePacket(VerifyStatus::VERIFIED()), $client);
            Network::getInstance()->broadcastPacket(new ServerSyncPacket($server), $client);
            $server->sync();
            $server->setServerStatus(ServerStatus::ONLINE());
            $server->sendPacket(new KeepAlivePacket());
        } else $this->sendResponse(new LoginResponsePacket(VerifyStatus::DENIED()), $client);
    }
}