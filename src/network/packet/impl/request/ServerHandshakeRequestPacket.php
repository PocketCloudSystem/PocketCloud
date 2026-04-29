<?php

namespace pocketcloud\cloud\network\packet\impl\request;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\server\ServerPostVerificationEvent;
use pocketcloud\cloud\event\impl\server\ServerVerifyEvent;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\data\VerifyStatus;
use pocketcloud\cloud\network\packet\impl\response\ServerHandshakeResponsePacket;
use pocketcloud\cloud\network\packet\impl\ServerSyncPacket;
use pocketcloud\cloud\network\packet\RequestPacket;
use pocketcloud\cloud\network\packet\util\PacketData;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\util\ServerStartSnapshot;
use pocketcloud\cloud\server\util\ServerStatus;

final class ServerHandshakeRequestPacket extends RequestPacket {

    public function __construct(
        private string $serverName = "",
        private int $processId = -1,
        private int $maxPlayers = -1,
    ) {}

    public function handle(ServerClient $client): void {
        if (($server = CloudServerManager::getInstance()->get($this->serverName)) !== null && ServerClientCache::getInstance()->getServer($client) === null) {
            ($ev = new ServerVerifyEvent($server))->call();
            if ($ev->isCancelled()) {
                CloudLogger::get()->warn("Denied server handshake request from §b{} §8(§b{}§8)", $this->serverName, $client->getAddress());
                return;
            }

            $post = ServerStartSnapshot::capture();
            $pre = $server->getPreStartSnapshot();
            $elapsed = round($post->capturedAt - $pre->capturedAt, 2);

            CloudLogger::get()->debug(
                "§b{}§r started in §a{}s§r | TPS: §e{}§r→§e{}§r | TickUsage: §e{}%§r→§e{}%§r | CPU: §e{}%§r→§e{}%§r | RAM: §e{}MB§r→§e{}MB",
                $server->getName(), $elapsed,
                round($pre->avgTps, 1), round($post->avgTps, 1),
                round($pre->tickUsage, 1), round($post->tickUsage, 1),
                round($pre->cpuUsage, 1), round($post->cpuUsage, 1),
                round($pre->memoryUsage), round($post->memoryUsage)
            );

            $server->setPostStartSnapshot($post);
            ServerClientCache::getInstance()->add($server, $client);
            CloudLogger::get()->success("The server §b{} §rhas §aconnected §rto the cloud. §8(§rTook §b{}§rs§8)", $server->getName(), round(microtime(true) - $server->getStartTime(), 3));
            $server->getServerData()->setMaxPlayers($this->maxPlayers);
            $server->getServerData()->setProcessId($this->processId);
            $server->setVerifyStatus(VerifyStatus::VERIFIED);
            $server->addToProxies();
            $server->sync();
            new ServerPostVerificationEvent($server)->call();
            $this->sendResponse(new ServerHandshakeResponsePacket(VerifyStatus::VERIFIED), $client);
            $server->setServerStatus(ServerStatus::ONLINE);
        } else {
            CloudLogger::get()->warn("Denied server handshake request from §b{} §8(§b{}§8)§r, duplicate server...", $this->serverName, $client->getAddress());
            $this->sendResponse(new ServerHandshakeResponsePacket(VerifyStatus::DENIED), $client);
        }
    }

    public function decodePayload(PacketData $packetData): void {
        $packetData->readAll($this->serverName, $this->processId, $this->maxPlayers);
    }

    public function getServerName(): string {
        return $this->serverName;
    }

    public function getProcessId(): int {
        return $this->processId;
    }

    public function getMaxPlayers(): int {
        return $this->maxPlayers;
    }

    public static function create(string $serverName, int $processId, int $maxPlayers): self {
        return new self($serverName, $processId, $maxPlayers);
    }
}