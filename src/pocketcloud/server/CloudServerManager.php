<?php

namespace pocketcloud\server;

use pocketcloud\event\impl\server\ServerCantStartEvent;
use pocketcloud\event\impl\server\ServerCrashEvent;
use pocketcloud\event\impl\server\ServerSaveEvent;
use pocketcloud\event\impl\server\ServerSendCommandEvent;
use pocketcloud\event\impl\server\ServerStartEvent;
use pocketcloud\event\impl\server\ServerStopEvent;
use pocketcloud\event\impl\server\ServerTimeOutEvent;
use pocketcloud\language\Language;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\CommandSendPacket;
use pocketcloud\network\packet\impl\normal\DisconnectPacket;
use pocketcloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\network\packet\impl\normal\ProxyUnregisterServerPacket;
use pocketcloud\network\packet\impl\normal\ServerSyncPacket;
use pocketcloud\network\packet\impl\types\DisconnectReason;
use pocketcloud\network\packet\impl\types\NotifyType;
use pocketcloud\PocketCloud;
use pocketcloud\promise\Promise;
use pocketcloud\server\crash\CrashChecker;
use pocketcloud\server\data\CloudServerData;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\server\utils\IdManager;
use pocketcloud\server\utils\PortManager;
use pocketcloud\server\utils\PropertiesMaker;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateType;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\SingletonTrait;
use pocketcloud\util\Tickable;
use pocketcloud\util\Utils;

class CloudServerManager implements Tickable {
    use SingletonTrait;

    /** @var array<CloudServer> */
    private array $servers = [];

    public function startServer(Template $template, int $count = 1): void {
        if (count($this->getServersByTemplate($template)) >= $template->getMaxServerCount()) {
            CloudLogger::get()->info(Language::current()->translate("server.max.reached", $template->getName()));
        } else {
            for ($i = 0; $i < $count; $i++) {
                if (count($this->getServersByTemplate($template)) >= $template->getMaxServerCount()) break;
                $id = IdManager::getFreeId($template);
                if ($id !== -1) {
                    $port = ($template->getTemplateType() === TemplateType::SERVER() ? PortManager::getFreePort() : PortManager::getFreeProxyPort());
                    if ($port !== 0) {
                        $server = new CloudServer($id, $template, new CloudServerData($port, $template->getMaxPlayerCount(), 0), ServerStatus::STARTING());
                        if (!file_exists($server->getPath())) {
                            Utils::copyDir($template->getPath(), $server->getPath());
                        } else {
                            if (!$template->isStatic()) Utils::deleteDir($server->getPath());
                            Utils::copyDir($template->getPath(), $server->getPath());
                        }

                        if ($template->getTemplateType() === TemplateType::SERVER()) Utils::copyDir(SERVER_PLUGINS_PATH, $server->getPath() . "plugins/");
                        else Utils::copyDir(PROXY_PLUGINS_PATH, $server->getPath() . "plugins/");

                        PropertiesMaker::copyProperties($server);

                        $this->addServer($server);

                        (new ServerStartEvent($server))->call();
                        CloudLogger::get()->info(Language::current()->translate("server.starting", $server->getName()));
                        NotifyType::STARTING()->notify(["%server%" => $server->getName()]);
                        Utils::executeWithStartCommand($server->getPath(), $server->getName(), $template->getTemplateType()->getSoftware()->getStartCommand());
                    }
                }
            }
        }
    }

    public function stopServer(CloudServer $server, bool $force = false): void {
        (new ServerStopEvent($server, $force))->call();
        CloudLogger::get()->info(Language::current()->translate("server.stopping", $server->getName()));
        NotifyType::STOPPING()->notify(["%server%" => $server->getName()]);
        $server->setServerStatus(ServerStatus::STOPPING());
        $server->setStopTime(time());
        if ($force) {
            if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());
            if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
        } else {
            $server->sendPacket(new DisconnectPacket(DisconnectReason::SERVER_SHUTDOWN()));
        }
    }

    public function stopTemplate(Template $template, bool $force = false): void {
        if (empty($this->getServersByTemplate($template))) {
            CloudLogger::get()->info(Language::current()->translate("server.none", $template->getName()));
            return;
        }

        foreach ($this->getServersByTemplate($template) as $server) $this->stopServer($server, $force);
    }

    public function stopAll(bool $force = false): void {
        foreach ($this->getServers() as $server) $this->stopServer($server, $force);
    }

    public function saveServer(CloudServer $server): void {
        $ev = new ServerSaveEvent($server);
        $ev->call();

        if ($ev->isCancelled()) {
            CloudLogger::get()->info(Language::current()->translate("server.saving.failed", $server->getName()));
            return;
        }

        CloudLogger::get()->info(Language::current()->translate("server.saving", $server->getName()));
        $startTime = microtime(true);
        $this->sendCommand($server, "save-all")->then(function() use($startTime, $server): void {
            $this->instantSave($server);
            CloudLogger::get()->info(Language::current()->translate("server.saved", $server->getName(), number_format(microtime(true) - $startTime, 3)));
        });
    }

    public function instantSave(CloudServer $server): void {
        Utils::copyDir($server->getPath() . "players/", $server->getTemplate()->getPath() . "players/");
        Utils::copyDir($server->getPath() . "plugin_data/", $server->getTemplate()->getPath() . "plugin_data/");
        Utils::copyDir($server->getPath() . "worlds/", $server->getTemplate()->getPath() . "worlds/");
        Utils::copyDir($server->getPath() . "players/", $server->getTemplate()->getPath() . "players/");
        Utils::copyFile($server->getPath() . "ops.txt", $server->getTemplate()->getPath() . "ops.txt");
        Utils::copyFile($server->getPath() . "banned-players.txt", $server->getTemplate()->getPath() . "banned-players.txt");
        Utils::copyFile($server->getPath() . "banned-ips.txt", $server->getTemplate()->getPath() . "banned-ips.txt");
        Utils::copyFile($server->getPath() . "pocketmine.yml", $server->getTemplate()->getPath() . "pocketmine.yml");
        Utils::copyFile($server->getPath() . "white-list.txt", $server->getTemplate()->getPath() . "white-list.txt");
    }

    public function sendCommand(CloudServer $server, string $commandLine): ?Promise {
        $ev = new ServerSendCommandEvent($server, $commandLine);
        $ev->call();

        if ($ev->isCancelled()) return null;

        if (!$server->sendPacket(new CommandSendPacket($commandLine))) return null;
        $promise = new Promise();
        $server->getCloudServerStorage()->put("command_promise_time", time());
        $server->getCloudServerStorage()->put("command_promise", $promise);
        return $promise;
    }

    public function addServer(CloudServer $server): void {
        if (!isset($this->servers[$server->getName()])) $this->servers[$server->getName()] = $server;
        IdManager::addId($server->getTemplate(), $server->getId());
        PortManager::addPort($server->getCloudServerData()->getPort());
        if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            foreach (array_filter($this->getServers(), fn(CloudServer $server) => $server->getTemplate()->getTemplateType() === TemplateType::PROXY()) as $proxyServer) {
                if (($client = ServerClientManager::getInstance()->getClientOfServer($proxyServer)) !== null) {
                    $client->sendPacket(new ProxyRegisterServerPacket($server->getName(), $server->getCloudServerData()->getPort()));
                }
            }
        }
    }

    public function removeServer(CloudServer $server): void {
        if (isset($this->servers[$server->getName()])) unset($this->servers[$server->getName()]);
        IdManager::removeId($server->getTemplate(), $server->getId());
        PortManager::removePort($server->getCloudServerData()->getPort());
        Network::getInstance()->broadcastPacket(new ServerSyncPacket($server, true));
        if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            if (PropertiesMaker::getProperties($server->getTemplate())->get("auto-save") == "on") $this->instantSave($server);
            foreach (array_filter($this->getServers(), fn(CloudServer $server) => $server->getTemplate()->getTemplateType() === TemplateType::PROXY()) as $proxyServer) {
                if (($client = ServerClientManager::getInstance()->getClientOfServer($proxyServer)) !== null) {
                    $client->sendPacket(new ProxyUnregisterServerPacket($server->getName()));
                }
            }
        }
    }

    public function tick(int $currentTick): void {
        if (PocketCloud::getInstance()->isReloading()) return;
        foreach ($this->getServers() as $server) {
            if ($server->getCloudServerStorage()->has("command_promise_time")) {
                $promise = $server->getCloudServerStorage()->get("command_promise");
                if ($promise instanceof Promise) {
                    if (($server->getCloudServerStorage()->get("command_promise_time") + CloudServer::TIMEOUT) <= time()) {
                        $promise->reject();
                        $server->getCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
                    }
                }
            }

            if ($server->getServerStatus() === ServerStatus::STARTING()) {
                if (($server->getStartTime() + CloudServer::TIMEOUT) <= time()) {
                    (new ServerCantStartEvent($server))->call();
                    if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());
                    $this->removeServer($server);
                    ServerClientManager::getInstance()->removeClient($server);
                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info(Language::current()->translate("server.starting.failed.crashed", $server->getName()));
                        $this->printServerStackTrace($server->getName(), $crashData);
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                    } else {
                        CloudLogger::get()->info(Language::current()->translate("server.starting.failed", $server->getName()));
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) Utils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else Utils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }
                    NotifyType::START_FAILED()->notify(["%server%" => $server->getName()]);
                    if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
                }
            } else if ($server->getServerStatus() === ServerStatus::ONLINE() || $server->getServerStatus() === ServerStatus::FULL() || $server->getServerStatus() === ServerStatus::IN_GAME()) {
                if (!$server->checkAlive()) {
                    (new ServerTimeOutEvent($server))->call();
                    if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());
                    $this->removeServer($server);
                    ServerClientManager::getInstance()->removeClient($server);
                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info(Language::current()->translate("server.crashed", $server->getName()));
                        $this->printServerStackTrace($server->getName(), $crashData);
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                        NotifyType::CRASHED()->notify(["%server%" => $server->getName()]);
                    } else {
                        CloudLogger::get()->info(Language::current()->translate("server.timed", $server->getName()));
                        NotifyType::TIMED()->notify(["%server%" => $server->getName()]);
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) Utils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else Utils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }
                    if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
                }
            } else if ($server->getServerStatus() === ServerStatus::STOPPING()) {
                if (($server->getStopTime() + 10) <= time()) {
                    $this->removeServer($server);
                    ServerClientManager::getInstance()->removeClient($server);
                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info(Language::current()->translate("server.crashed", $server->getName()));
                        $this->printServerStackTrace($server->getName(), $crashData);
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                    } else {
                        CloudLogger::get()->info(Language::current()->translate("server.stopping.failed", $server->getName()));
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) Utils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else Utils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }
                    if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
                    Utils::kill($server->getCloudServerData()->getProcessId());
                }
            } else if ($server->getServerStatus() === ServerStatus::OFFLINE()) {
                $this->removeServer($server);
                ServerClientManager::getInstance()->removeClient($server);

                if (CrashChecker::checkCrashed($server, $crashData)) {
                    CloudLogger::get()->info(Language::current()->translate("server.crashed", $server->getName()));
                    $this->printServerStackTrace($server->getName(), $crashData);
                    (new ServerCrashEvent($server, $crashData))->call();
                    CrashChecker::writeCrashFile($server, $crashData);
                    NotifyType::CRASHED()->notify(["%server%" => $server->getName()]);
                }

                if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
            }
        }
    }

    /** @internal */
    public function printServerStackTrace(string $server, array $crashData): void {
        CloudLogger::get()->info("§8[§cERROR§8/§e%s§r§8] §cUnhandled §e%s§c: §e%s §cwas thrown in §e%s §cat line §e%s", $server, $crashData["error"]["type"], $crashData["error"]["message"] ?? "Unknown error", $crashData["error"]["file"], $crashData["error"]["line"]);
        foreach ($crashData["trace"] as $message) CloudLogger::get()->debug("§c" . $message);
    }

    public function getServerByName(string $name): ?CloudServer {
        return $this->servers[$name] ?? null;
    }

    public function getServersByTemplate(Template $template): array {
        return array_filter($this->servers, fn(CloudServer $server) => $server->getTemplate() === $template);
    }

    public function getServers(): array {
        return $this->servers;
    }
}