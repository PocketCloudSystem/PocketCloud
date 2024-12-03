<?php

namespace pocketcloud\cloud\server;

use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\event\impl\server\ServerCrashEvent;
use pocketcloud\cloud\event\impl\server\ServerSaveEvent;
use pocketcloud\cloud\event\impl\server\ServerSendCommandEvent;
use pocketcloud\cloud\event\impl\server\ServerStartFailEvent;
use pocketcloud\cloud\event\impl\server\ServerTimeOutEvent;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\impl\type\CommandExecutionResult;
use pocketcloud\cloud\network\packet\impl\type\NotifyType;
use pocketcloud\cloud\server\crash\CrashChecker;
use pocketcloud\cloud\server\data\CloudServerData;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\terminal\TerminalUtils;
use pocketcloud\cloud\util\tick\Tickable;

final class CloudServerManager implements Tickable {
    use SingletonTrait;
    
    /** @var array<CloudServer> */
    private array $servers = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function start(Template $template, int $count = 1): ?array {
        $startedServers = [];
        if (count($this->getAllByTemplate($template)) >= $template->getSettings()->getMaxServerCount()) {
            CloudLogger::get()->warn("Can not start any more servers of §e" . $template->getName() . " §rdue to the max servers reached.");
            return null;
        } else {
            for ($i = 0; $i < $count; $i++) {
                if (count($this->getAllByTemplate($template)) >= $template->getSettings()->getMaxServerCount()) break;
                $id = ServerUtils::getFreeId($template);
                if ($id !== -1) {
                    $port = ($template->getTemplateType() === TemplateType::SERVER() ? ServerUtils::getFreePort() : ServerUtils::getFreeProxyPort());
                    if ($port !== 0) {
                        $server = new CloudServer($id, $template->getName(), new CloudServerData($port, $template->getSettings()->getMaxPlayerCount(), 0), ServerStatus::STARTING());
                        $server->prepare();
                        $server->start();
                        $startedServers[] = $server->getName();
                    }
                }
            }
        }
        return $startedServers;
    }

    public function stop(Template|CloudServer|string $object, bool $force = false): void {
        $object = is_string($object) ? (
            $this->get($object) ?? TemplateManager::getInstance()->get($object)
        ) : $object;

        if ($object instanceof Template) {
            foreach ($this->getAllByTemplate($object) as $server) $this->stop($server, $force);
        } else if ($object instanceof CloudServer) {
            $object->stop($force);
        }
    }

    public function stopAll(bool $force = false): void {
        foreach ($this->getAll() as $server) $this->stop($server, $force);
    }

    public function save(CloudServer $server): void {
        $ev = new ServerSaveEvent($server);
        $ev->call();

        if ($ev->isCancelled()) {
            CloudLogger::get()->warn("Failed to save the server files of §e" . $server->getName() . " §rdue to the event being §ccancelled§r.");
            return;
        }

        CloudLogger::get()->info("Trying to save the server §e" . $server->getName() . "§r...");
        $startTime = microtime(true);
        $this->send($server, "save-all")->then(function() use($startTime, $server): void {
            $this->instantSave($server);
            CloudLogger::get()->info("Successfully §asaved §rthe server files of §e" . $server->getName() . " §rin §e" . number_format(microtime(true) - $startTime, 3) . "s§r.");
        });
    }

    public function instantSave(CloudServer $server): void {
        CloudLogger::get()->debug("Copying files from " . $server->getPath() . " to " . $server->getTemplate()->getPath() . "...");

        if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            FileUtils::copyDirectory($server->getPath() . "players/", $server->getTemplate()->getPath() . "players/");
            FileUtils::copyDirectory($server->getPath() . "plugin_data/", $server->getTemplate()->getPath() . "plugin_data/");
            FileUtils::copyDirectory($server->getPath() . "worlds/", $server->getTemplate()->getPath() . "worlds/");
            FileUtils::copyFile($server->getPath() . "ops.txt", $server->getTemplate()->getPath() . "ops.txt");
            FileUtils::copyFile($server->getPath() . "banned-players.txt", $server->getTemplate()->getPath() . "banned-players.txt");
            FileUtils::copyFile($server->getPath() . "banned-ips.txt", $server->getTemplate()->getPath() . "banned-ips.txt");
            FileUtils::copyFile($server->getPath() . "pocketmine.yml", $server->getTemplate()->getPath() . "pocketmine.yml");
            FileUtils::copyFile($server->getPath() . "white-list.txt", $server->getTemplate()->getPath() . "white-list.txt");
        } else {
            FileUtils::copyDirectory($server->getPath() . "config.yml", $server->getTemplate()->getPath() . "config.yml");
            FileUtils::copyDirectory($server->getPath() . "lang.ini", $server->getTemplate()->getPath() . "lang.ini");
        }
    }

    public function send(CloudServer $server, string $commandLine, bool $internal = false, ?ICommandSender $internalSender = null): ?Promise {
        $ev = new ServerSendCommandEvent($server, $commandLine);
        $ev->call();

        if ($ev->isCancelled()) return null;

        //TODO: send packet
        $promise = new Promise();
        $server->getInternalCloudServerStorage()->set("command_promise_time", time());
        $server->getInternalCloudServerStorage()->set("command_promise", $promise);
        if ($internal && $internalSender !== null) $promise->then(function(CommandExecutionResult $result) use($server, $internalSender): void {
            $server->getInternalCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
            $internalSender->info("The server §e" . $server->getName() . " §rsuccessfully handled the command and respond with:");
            if (empty($result->getMessages())) $internalSender->info("§c/");
            else foreach ($result->getMessages() as $message) $internalSender->info("§e" . $server->getName() . "§8: §r" . $message);
        })->failure(function() use($server, $internalSender): void {
            $server->getInternalCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
            $internalSender->warn("Failed to send the command to the server §e" . $server->getName() . "§r...");
        });

        return $promise;
    }

    public function add(CloudServer $server): void {
        if (!isset($this->servers[$server->getName()])) $this->servers[$server->getName()] = $server;
        ServerUtils::addId($server->getTemplate(), $server->getId());
        ServerUtils::addPort($server->getCloudServerData()->getPort());

        if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            foreach (array_filter($this->getAll(), fn(CloudServer $server) => $server->getTemplate()->getTemplateType() === TemplateType::PROXY()) as $proxyServer) {
                if (($client = ServerClientCache::getInstance()->get($proxyServer)) !== null) {
                    //TODO: register at proxy
                }
            }
        }
    }

    public function remove(CloudServer $server): void {
        if (isset($this->servers[$server->getName()])) unset($this->servers[$server->getName()]);
        ServerUtils::removeId($server->getTemplate(), $server->getId());
        ServerUtils::removePort($server->getCloudServerData()->getPort());
        //TODO: Send sync packet

        if ($server->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            if (ServerUtils::getProperties($server->getTemplate())->get("auto-save") == "on") $this->instantSave($server);
            foreach (array_filter($this->getAll(), fn(CloudServer $server) => $server->getTemplate()->getTemplateType() === TemplateType::PROXY()) as $proxyServer) {
                if (($client = ServerClientCache::getInstance()->get($proxyServer)) !== null) {
                    //TODO: unregister at proxy
                }
            }
        }
    }

    public function tick(int $currentTick): void {
        foreach ($this->getAll() as $server) {
            if ($server->getInternalCloudServerStorage()->has("command_promise_time")) {
                $promise = $server->getInternalCloudServerStorage()->get("command_promise");
                if ($promise instanceof Promise) {
                    if (($server->getInternalCloudServerStorage()->get("command_promise_time") + ServerUtils::TIMEOUT_SERVER) <= time()) {
                        $promise->reject();
                        $server->getInternalCloudServerStorage()->remove("command_promise")->remove("command_promise_time");
                    }
                }
            }

            if ($server->getServerStatus() === ServerStatus::STARTING()) {
                $timeout = match ($server->getTemplate()->getTemplateType()->isServer()) {
                    true => ServerUtils::TIMEOUT_SERVER,
                    default => ServerUtils::TIMEOUT_PROXY
                };

                if (($server->getStartTime() + $timeout) <= time()) {
                    (new ServerStartFailEvent($server))->call();
                    if ($server->getCloudServerData()->getProcessId() !== 0) TerminalUtils::kill($server->getCloudServerData()->getProcessId());
                    $this->remove($server);
                    ServerClientCache::getInstance()->remove($server);

                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->warn("Failed to start server §e" . $server->getName() . "§r, writing crash file...");
                        $this->printServerStackTrace($server->getName(), $crashData);
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                    } else {
                        CloudLogger::get()->warn("Failed to start the server §e" . $server->getName() . "§r, deleting it's data...");
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) FileUtils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else FileUtils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }

                    NotifyType::START_FAILED()->send(["%server%" => $server->getName()]);
                    if (!$server->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($server->getPath());
                }
            } else if ($server->getServerStatus() === ServerStatus::ONLINE() || $server->getServerStatus() === ServerStatus::FULL() || $server->getServerStatus() === ServerStatus::IN_GAME()) {
                if (!$server->checkAlive()) {
                    (new ServerTimeOutEvent($server))->call();
                    if ($server->getCloudServerData()->getProcessId() !== 0) TerminalUtils::kill($server->getCloudServerData()->getProcessId());
                    $this->remove($server);
                    ServerClientCache::getInstance()->remove($server);

                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §ccrashed§r, writing crash file...");
                        $this->printServerStackTrace($server->getName(), $crashData);
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                        NotifyType::CRASHED()->send(["%server%" => $server->getName()]);
                    } else {
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §rhas §ctimed out§r, deleting it's data...");
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) FileUtils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else FileUtils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                        NotifyType::TIMED()->send(["%server%" => $server->getName()]);
                    }

                    if (!$server->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($server->getPath());
                }
            } else if ($server->getServerStatus() === ServerStatus::STOPPING()) {
                if (($server->getStopTime() + 10) <= time()) {
                    $this->remove($server);
                    ServerClientCache::getInstance()->remove($server);
                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §ccrashed§r!");
                        $this->printServerStackTrace($server->getName(), $crashData);
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                    } else {
                        CloudLogger::get()->warn("Failed to stop the server §e" . $server->getName() . "§r, killing the process instead...");
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) FileUtils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else FileUtils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }

                    NotifyType::CRASHED()->send(["%server%" => $server->getName()]);
                    if (!$server->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($server->getPath());
                    TerminalUtils::kill($server->getCloudServerData()->getProcessId());
                }
            } else if ($server->getServerStatus() === ServerStatus::OFFLINE()) {
                $this->remove($server);
                ServerClientCache::getInstance()->remove($server);

                if (CrashChecker::checkCrashed($server, $crashData)) {
                    CloudLogger::get()->info("The server §e" . $server->getName() . " §ccrashed§r!");
                    $this->printServerStackTrace($server->getName(), $crashData);
                    (new ServerCrashEvent($server, $crashData))->call();
                    CrashChecker::writeCrashFile($server, $crashData);
                    NotifyType::CRASHED()->send(["%server%" => $server->getName()]);
                }

                if (!$server->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($server->getPath());
            }
        }
    }

    /** @internal */
    public function printServerStackTrace(string $server, array $crashData): void {
        CloudLogger::get()->info("§8[§cERROR§8/§e%s§r§8] §cUnhandled §e%s§c: §e%s §cwas thrown in §e%s §cat line §e%s", $server, $crashData["error"]["type"], $crashData["error"]["message"] ?? "Unknown error", $crashData["error"]["file"], $crashData["error"]["line"]);
        foreach ($crashData["trace"] as $message) CloudLogger::get()->error("§c" . $message);
    }

    public function get(string $name): ?CloudServer {
        return $this->servers[$name] ?? null;
    }

    /** @return array<CloudServer> */
    public function getAllByTemplate(Template $template): array {
        return array_filter($this->servers, fn(CloudServer $server) => $server->getTemplate() === $template);
    }

    public function getAll(): array {
        return $this->servers;
    }
}