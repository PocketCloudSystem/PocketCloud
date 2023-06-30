<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\event\impl\server\ServerCrashEvent;
use pocketcloud\event\impl\server\ServerDisconnectEvent;
use pocketcloud\language\Language;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\network\packet\impl\types\DisconnectReason;
use pocketcloud\network\packet\utils\PacketData;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\crash\CrashChecker;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\Utils;

class DisconnectPacket extends CloudPacket {

    public function __construct(private ?DisconnectReason $disconnectReason = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeDisconnectReason($this->disconnectReason);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->disconnectReason = $packetData->readDisconnectReason();
    }

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            if ($server->getServerStatus() === ServerStatus::OFFLINE()) {
                if (isset(CloudServerManager::getInstance()->getServers()[$server->getName()])) CloudServerManager::getInstance()->removeServer($server);
                return;
            }

            $server->setServerStatus(ServerStatus::OFFLINE());
            (new ServerDisconnectEvent($server))->call();
            if (CrashChecker::checkCrashed($server, $crashData)) {
                (new ServerCrashEvent($server, $crashData))->call();
                CloudLogger::get()->info(Language::current()->translate("server.crashed", $server->getName()));
                CloudServerManager::getInstance()->printServerStackTrace($server->getName(), $crashData);
                CrashChecker::writeCrashFile($server, $crashData);
            } else {
                CloudLogger::get()->info(Language::current()->translate("server.stopped", $server->getName()));
            }

            if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());

            ServerClientManager::getInstance()->removeClient($server);
            CloudServerManager::getInstance()->removeServer($server);
            if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
        }
    }

    public function getDisconnectReason(): ?DisconnectReason {
        return $this->disconnectReason;
    }
}