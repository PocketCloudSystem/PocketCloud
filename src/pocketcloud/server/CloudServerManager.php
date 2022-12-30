<?php

namespace pocketcloud\server;

use pocketcloud\event\impl\server\ServerSaveEvent;
use pocketcloud\event\impl\server\ServerSendCommandEvent;
use pocketcloud\event\impl\server\ServerStartEvent;
use pocketcloud\event\impl\server\ServerStopEvent;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\Network;
use pocketcloud\network\packet\impl\normal\CommandSendPacket;
use pocketcloud\network\packet\impl\normal\DisconnectPacket;
use pocketcloud\network\packet\impl\normal\LocalServerUnregisterPacket;
use pocketcloud\network\packet\impl\normal\ProxyRegisterServerPacket;
use pocketcloud\network\packet\impl\normal\ProxyUnregisterServerPacket;
use pocketcloud\network\packet\impl\types\DisconnectReason;
use pocketcloud\notify\Notifier;
use pocketcloud\notify\NotifyMessage;
use pocketcloud\scheduler\ClosureTask;
use pocketcloud\scheduler\TaskScheduler;
use pocketcloud\server\data\CloudServerData;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\server\utils\IdManager;
use pocketcloud\server\utils\PortManager;
use pocketcloud\server\utils\PropertiesMaker;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\Utils;

class CloudServerManager {
    use SingletonTrait;

    /** @var array<CloudServer> */
    private array $servers = [];

    public function startServer(Template $template, int $count = 1) {
        if (count($this->getServersByTemplate($template)) >= $template->getMaxServerCount()) {
            CloudLogger::get()->error("§cNo servers from the §e" . $template->getName() . " §ctemplate can be started anymore because the limit was reached!");
        } else {
            for ($i = 0; $i < $count; $i++) {
                if (count($this->getServersByTemplate($template)) >= $template->getMaxServerCount()) break;
                $id = IdManager::getFreeId($template);
                if ($id !== -1) {
                    $port = ($template->getTemplateType() === TemplateType::SERVER() ? PortManager::getFreePort() : PortManager::getFreeProxyPort());
                    if ($port !== 0) {
                        $server = new CloudServer($id, $template, new CloudServerData($port, $template->getMaxPlayerCount(), 0), ServerStatus::STARTING());
                        if (file_exists($server->getPath()) && !$template->isStatic()) Utils::deleteDir($server->getPath());
                        if (!file_exists($server->getPath())) {
                            mkdir($server->getPath());
                            Utils::copyDir($template->getPath(), $server->getPath());
                        }

                        if ($template->getTemplateType() === TemplateType::SERVER()) Utils::copyDir(SERVER_PLUGINS_PATH, $server->getPath() . "plugins/");
                        else Utils::copyDir(PROXY_PLUGINS_PATH, $server->getPath() . "plugins/");

                        PropertiesMaker::copyProperties($server);

                        $this->addServer($server);

                        (new ServerStartEvent($server))->call();
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §ris §astarting§r...");
                        Notifier::sendNotify(NotifyMessage::SERVER_START()->withReplacements(["server" => $server->getName()]));
                        Utils::executeWithStartCommand($server->getPath(), $server->getName(), $template->getTemplateType()->getSoftware()->getStartCommand());
                    }
                }
            }
        }
    }

    public function stopServer(CloudServer $server, bool $force = false) {
        (new ServerStopEvent($server, $force))->call();
        CloudLogger::get()->info("The server §e" . $server->getName() . " §ris §cstopping§r...");
        Notifier::sendNotify(NotifyMessage::SERVER_STOP()->withReplacements(["server" => $server->getName()]));
        $server->setServerStatus(ServerStatus::STOPPING());
        $server->setStopTime(microtime(true));
        if ($force) {
            if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());
            if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
        } else {
            $server->sendPacket(new DisconnectPacket(DisconnectReason::SERVER_SHUTDOWN()));
        }
    }

    public function stopTemplate(Template $template, bool $force = false) {
        if (empty($this->getServersByTemplate($template))) {
            CloudLogger::get()->error("§cNo servers available with the template §e" . $template->getName() . "§c!");
            return;
        }

        foreach ($this->getServersByTemplate($template) as $server) $this->stopServer($server, $force);
    }

    public function stopAll(bool $force = false) {
        foreach ($this->getServers() as $server) $this->stopServer($server, $force);
    }

    public function saveServer(CloudServer $server) {
        $ev = new ServerSaveEvent($server);
        $ev->call();

        if ($ev->isCancelled()) {
            CloudLogger::get()->info("§cCan't §rsave the server §e" . $server->getName() . "§r!");
            return;
        }

        CloudLogger::get()->info("Saving server §e" . $server->getName() . "§r...");
        $startTime = microtime(true);
        $this->sendCommand($server, "save-all");

        TaskScheduler::getInstance()->scheduleDelayedTask(new ClosureTask(function() use($startTime, $server): void {
            $this->instantSave($server);
            CloudLogger::get()->info("Saved the server §e" . $server->getName() . " §rin §e" . number_format(microtime(true) - $startTime, 3) . "s§r!");
        }), 2);
    }

    public function instantSave(CloudServer $server) {
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

    public function sendCommand(CloudServer $server, string $commandLine): bool {
        $ev = new ServerSendCommandEvent($server, $commandLine);
        $ev->call();

        if ($ev->isCancelled()) return false;
        return $server->sendPacket(new CommandSendPacket($commandLine));
    }

    public function addServer(CloudServer $server) {
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

    public function removeServer(CloudServer $server) {
        if (isset($this->servers[$server->getName()])) unset($this->servers[$server->getName()]);
        IdManager::removeId($server->getTemplate(), $server->getId());
        PortManager::removePort($server->getCloudServerData()->getPort());
        Network::getInstance()->broadcastPacket(new LocalServerUnregisterPacket($server->getName()));
        if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            if (PropertiesMaker::getProperties($server->getTemplate())->get("auto-save") == "on") $this->instantSave($server);
            foreach (array_filter($this->getServers(), fn(CloudServer $server) => $server->getTemplate()->getTemplateType() === TemplateType::PROXY()) as $proxyServer) {
                if (($client = ServerClientManager::getInstance()->getClientOfServer($proxyServer)) !== null) {
                    $client->sendPacket(new ProxyUnregisterServerPacket($server->getName()));
                }
            }
        }
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