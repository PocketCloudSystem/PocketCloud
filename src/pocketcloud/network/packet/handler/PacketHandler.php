<?php

namespace pocketcloud\network\packet\handler;

use pocketcloud\config\MaintenanceConfig;
use pocketcloud\config\NotifyConfig;
use pocketcloud\event\impl\network\NetworkPacketReceiveEvent;
use pocketcloud\event\impl\player\PlayerSwitchServerEvent;
use pocketcloud\event\impl\server\ServerCrashEvent;
use pocketcloud\event\impl\server\ServerDisconnectEvent;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\handler\decoder\PacketDecoder;
use pocketcloud\network\packet\impl\normal\CloudPlayerSwitchServerPacket;
use pocketcloud\network\packet\impl\normal\CloudServerSavePacket;
use pocketcloud\network\packet\impl\normal\CloudServerStatusChangePacket;
use pocketcloud\network\packet\impl\normal\DisconnectPacket;
use pocketcloud\network\packet\impl\normal\KeepALivePacket;
use pocketcloud\network\packet\impl\normal\LocalServerRegisterPacket;
use pocketcloud\network\packet\impl\normal\PlayerConnectPacket;
use pocketcloud\network\packet\impl\normal\PlayerDisconnectPacket;
use pocketcloud\network\packet\impl\normal\PlayerKickPacket;
use pocketcloud\network\packet\impl\normal\PlayerNotifyUpdatePacket;
use pocketcloud\network\packet\impl\normal\PlayerTextPacket;
use pocketcloud\network\packet\impl\request\CheckPlayerMaintenanceRequestPacket;
use pocketcloud\network\packet\impl\request\CheckPlayerNotifyRequestPacket;
use pocketcloud\network\packet\impl\request\CloudServerStartRequestPacket;
use pocketcloud\network\packet\impl\request\CloudServerStopRequestPacket;
use pocketcloud\network\packet\impl\request\LoginRequestPacket;
use pocketcloud\network\packet\impl\response\CheckPlayerMaintenanceResponsePacket;
use pocketcloud\network\packet\impl\response\CheckPlayerNotifyResponsePacket;
use pocketcloud\network\packet\impl\response\CloudServerStartResponsePacket;
use pocketcloud\network\packet\impl\response\CloudServerStopResponsePacket;
use pocketcloud\network\packet\impl\response\LoginResponsePacket;
use pocketcloud\network\packet\impl\types\ErrorReason;
use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\network\packet\listener\PacketListener;
use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\UnhandledPacketObject;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\crash\CrashChecker;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\Address;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\Utils;

class PacketHandler {
    use SingletonTrait;

    public function __construct() {
        self::setInstance($this);
        PacketListener::getInstance()->register(LoginRequestPacket::class, function(LoginRequestPacket $packet, ServerClient $client): void {
            if (($server = CloudServerManager::getInstance()->getServerByName($packet->getServerName())) !== null) {
                if (ServerClientManager::getInstance()->getClientOfServer($server) === null) {
                    ServerClientManager::getInstance()->addClient($server, $client);
                    CloudLogger::get()->info("The server §e" . $server->getName() . " §rhas successfully §aconnected §rto the cloud!");
                    $server->getCloudServerData()->setProcessId($packet->getProcessId());
                    $packet->sendResponse(new LoginResponsePacket(VerifyStatus::VERIFIED()), $client);
                    Network::getInstance()->broadcastPacket(new LocalServerRegisterPacket($server->toArray()), $client);
                    $server->sync();
                    $server->setServerStatus(ServerStatus::ONLINE());
                } else $packet->sendResponse(new LoginResponsePacket(VerifyStatus::NOT_VERIFIED()), $client);
            } else $packet->sendResponse(new LoginResponsePacket(VerifyStatus::NOT_VERIFIED()), $client);
        });

        PacketListener::getInstance()->register(DisconnectPacket::class, function(DisconnectPacket $packet, ServerClient $client): void {
            if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                if ($server->getServerStatus() === ServerStatus::OFFLINE()) {
                    if (isset(CloudServerManager::getInstance()->getServers()[$server->getName()])) CloudServerManager::getInstance()->removeServer($server);
                    return;
                }
                $server->setServerStatus(ServerStatus::OFFLINE());
                $server->setAlive(false);
                (new ServerDisconnectEvent($server))->call();
                if (CrashChecker::checkCrashed($server, $crashData)) {
                    (new ServerCrashEvent($server, $crashData))->call();
                    CloudLogger::get()->info("The server §e" . $server->getName() . " §rwas §ccrashed§r! Creating crashlog...");
                    CrashChecker::writeCrashFile($server, $crashData);
                } else {
                    CloudLogger::get()->info("The server §e" . $server->getName() . " §rwas §cstopped§r!");
                }

                if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());

                ServerClientManager::getInstance()->removeClient($server);
                CloudServerManager::getInstance()->removeServer($server);
                if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
            }
        });

        PacketListener::getInstance()->register(KeepALivePacket::class, function(KeepALivePacket $packet, ServerClient $client): void {
            if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                $server->setAlive(true);
            }
        });

        PacketListener::getInstance()->register(PlayerTextPacket::class, function(PlayerTextPacket $packet, ServerClient $client): void {
            Network::getInstance()->broadcastPacket($packet);
        });

        PacketListener::getInstance()->register(PlayerConnectPacket::class, function(PlayerConnectPacket $packet, ServerClient $client): void {
            if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                if (($player = CloudPlayerManager::getInstance()->getPlayerByName($packet->getPlayer()->getName())) === null) {
                    $player = $packet->getPlayer();
                    if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) $player->setCurrentServer($server);
                    else $player->setCurrentProxy($server);
                    CloudPlayerManager::getInstance()->addPlayer($player);
                } else {
                    if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) $player->setCurrentServer($server);
                    else $player->setCurrentProxy($server);
                }
            }
        });

        PacketListener::getInstance()->register(PlayerDisconnectPacket::class, function(PlayerDisconnectPacket $packet, ServerClient $client): void {
            if (($player = CloudPlayerManager::getInstance()->getPlayerByName($packet->getPlayer()->getName())) !== null) {
                if ($player->getCurrentProxy() === null) {
                    CloudPlayerManager::getInstance()->removePlayer($player);
                } else {
                    if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) {
                            CloudPlayerManager::getInstance()->removePlayer($player);
                        }
                    }
                }
            }
        });

        PacketListener::getInstance()->register(PlayerNotifyUpdatePacket::class, fn(PlayerNotifyUpdatePacket $packet, ServerClient $client) => NotifyConfig::getInstance()->edit($packet->getPlayer(), $packet->getValue()));
        PacketListener::getInstance()->register(CheckPlayerNotifyRequestPacket::class, fn(CheckPlayerNotifyRequestPacket $packet, ServerClient $client) => $packet->sendResponse(new CheckPlayerNotifyResponsePacket($packet->getPlayer(), NotifyConfig::getInstance()->is($packet->getPlayer())), $client));

        PacketListener::getInstance()->register(CloudServerStartRequestPacket::class, function(CloudServerStartRequestPacket $packet, ServerClient $client): void {
            if (($template = TemplateManager::getInstance()->getTemplateByName($packet->getTemplate())) !== null) {
                if (count(CloudServerManager::getInstance()->getServersByTemplate($template)) < $template->getMaxServerCount()) {
                    CloudServerManager::getInstance()->startServer($template, $packet->getCount());
                    $packet->sendResponse(new CloudServerStartResponsePacket(ErrorReason::NO_ERROR()), $client);
                } else $packet->sendResponse(new CloudServerStartResponsePacket(ErrorReason::MAX_SERVERS()), $client);
            } else $packet->sendResponse(new CloudServerStartResponsePacket(ErrorReason::TEMPLATE_EXISTENCE()), $client);
        });

        PacketListener::getInstance()->register(CloudServerStopRequestPacket::class, function(CloudServerStopRequestPacket $packet, ServerClient $client): void {
            if (($server = CloudServerManager::getInstance()->getServerByName($packet->getServer())) !== null) {
                CloudServerManager::getInstance()->stopServer($server);
                $packet->sendResponse(new CloudServerStopResponsePacket(ErrorReason::NO_ERROR()), $client);
            } else if (($template = TemplateManager::getInstance()->getTemplateByName($packet->getServer())) !== null) {
                CloudServerManager::getInstance()->stopTemplate($template);
                $packet->sendResponse(new CloudServerStopResponsePacket(ErrorReason::NO_ERROR()), $client);
            } else $packet->sendResponse(new CloudServerStopResponsePacket(ErrorReason::SERVER_EXISTENCE()), $client);
        });

        PacketListener::getInstance()->register(CloudServerSavePacket::class, function(CloudServerSavePacket $packet, ServerClient $client): void {
            if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                CloudServerManager::getInstance()->saveServer($server);
            }
        });

        PacketListener::getInstance()->register(CloudServerStatusChangePacket::class, function(CloudServerStatusChangePacket $packet, ServerClient $client): void {
            if (($server = ServerClientManager::getInstance()->getServerOfClient($client)) !== null) {
                $server->setServerStatus($packet->getNewStatus());
            }
        });

        PacketListener::getInstance()->register(CloudPlayerSwitchServerPacket::class, function(CloudPlayerSwitchServerPacket $packet, ServerClient $client): void {
            if (($player = CloudPlayerManager::getInstance()->getPlayerByName($packet->getPlayer())) !== null) {
                if (($server = CloudServerManager::getInstance()->getServerByName($packet->getNewServer())) !== null) {
                    Network::getInstance()->broadcastPacket($packet);
                    CloudLogger::get()->debug("Player " . $player->getName() . " performed a server switch (" . ($player->getCurrentServer()?->getName() ?? "NULL") . " -> " . ($server?->getName() ?? "NULL") . ")");
                    (new PlayerSwitchServerEvent($player, $player->getCurrentServer(), $server));
                    $player->setCurrentServer($server);
                }
            }
        });

        PacketListener::getInstance()->register(CheckPlayerMaintenanceRequestPacket::class, fn(CheckPlayerMaintenanceRequestPacket $packet, ServerClient $client) => $packet->sendResponse(new CheckPlayerMaintenanceResponsePacket($packet->getPlayer(), MaintenanceConfig::getInstance()->is($packet->getPlayer())), $client));

        PacketListener::getInstance()->register(PlayerKickPacket::class, function(PlayerKickPacket $packet, ServerClient $client): void {
            $player = CloudPlayerManager::getInstance()->getPlayerByName($packet->getPlayer());
            if ($player !== null) $player->kick($packet->getReason());
        });
    }

    public function handle(UnhandledPacketObject $object) {
        $buffer = $object->getBuffer();
        $address = $object->getAddress();
        $port = $object->getPort();
        if (($client = new ServerClient(new Address($address, $port)))->isLocalHost()) {
            if (($packet = PacketDecoder::decode($buffer)) !== null) {
                (new NetworkPacketReceiveEvent($packet, $client))->call();
                PacketListener::getInstance()->call($packet, $client);
            } else CloudLogger::get()->warn("§cReceived a unknown packet! §8(§e" . $client->getAddress() . "§8)");
        } else CloudLogger::get()->debug("§cReceived a buffer from an external socket client! §8(§e" . $client->getAddress() . "§8)");
    }
}