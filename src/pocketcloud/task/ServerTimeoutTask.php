<?php

namespace pocketcloud\task;

use pocketcloud\event\impl\server\ServerCantStartEvent;
use pocketcloud\event\impl\server\ServerCrashEvent;
use pocketcloud\event\impl\server\ServerTimeOutEvent;
use pocketcloud\network\client\ServerClientManager;
use pocketcloud\network\packet\impl\normal\KeepALivePacket;
use pocketcloud\notify\Notifier;
use pocketcloud\notify\NotifyMessage;
use pocketcloud\scheduler\Task;
use pocketcloud\server\CloudServerManager;
use pocketcloud\server\crash\CrashChecker;
use pocketcloud\server\status\ServerStatus;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\Utils;

class ServerTimeoutTask extends Task {

    public function onRun() {
        foreach (CloudServerManager::getInstance()->getServers() as $server) {
            if ($server->getServerStatus() === ServerStatus::STARTING()) {
                if (($server->getStartTime() + 20) <= microtime(true)) {
                    (new ServerCantStartEvent($server))->call();
                    if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());
                    CloudServerManager::getInstance()->removeServer($server);
                    ServerClientManager::getInstance()->removeClient($server);
                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §ccouldn't §rbe started §rbecause the server §ccrashed§r! Creating crashlog...");
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                    } else {
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §r§ccouldn't §rbe started!");
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) Utils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else Utils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }
                    Notifier::sendNotify(NotifyMessage::SERVER_COULD_NOT_START()->withReplacements(["server" => $server->getName()]));
                    if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
                }
            } else if ($server->getServerStatus() === ServerStatus::ONLINE() || $server->getServerStatus() === ServerStatus::FULL() || $server->getServerStatus() === ServerStatus::IN_GAME()) {
                if ($server->isFirstCheck()) {
                    $server->setAlive(false);
                    $server->setLastCheckTime(microtime(true));
                    $server->sendPacket(new KeepALivePacket());
                } else {
                    if ($server->isAlive()) {
                        $server->setAlive(false);
                        $server->setLastCheckTime(microtime(true));
                        $server->sendPacket(new KeepALivePacket());
                    } else {
                        if (($server->getLastCheckTime() + 10) <= microtime(true)) {
                            (new ServerTimeOutEvent($server))->call();
                            if ($server->getCloudServerData()->getProcessId() !== 0) Utils::kill($server->getCloudServerData()->getProcessId());
                            CloudServerManager::getInstance()->removeServer($server);
                            ServerClientManager::getInstance()->removeClient($server);
                            if (CrashChecker::checkCrashed($server, $crashData)) {
                                CloudLogger::get()->info("The server §e" . $server->getName() . " §rwas §ccrashed§r! Creating crashlog...");
                                (new ServerCrashEvent($server, $crashData))->call();
                                CrashChecker::writeCrashFile($server, $crashData);
                                Notifier::sendNotify(NotifyMessage::SERVER_CRASHED()->withReplacements(["server" => $server->getName()]));
                            } else {
                                CloudLogger::get()->info("The server §e" . $server->getName() . " §ris §ctimed out§r!");
                                Notifier::sendNotify(NotifyMessage::SERVER_TIMED_OUT()->withReplacements(["server" => $server->getName()]));
                                if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) Utils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                                else Utils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                            }
                            if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
                        }
                    }
                }
            } else if ($server->getServerStatus() === ServerStatus::STOPPING()) {
                if (($server->getStopTime() + 10) <= microtime(true)) {
                    CloudServerManager::getInstance()->removeServer($server);
                    ServerClientManager::getInstance()->removeClient($server);
                    if (CrashChecker::checkCrashed($server, $crashData)) {
                        CloudLogger::get()->info("The server §e" . $server->getName() . " §rwas §ccrashed§r! Creating crashlog...");
                        (new ServerCrashEvent($server, $crashData))->call();
                        CrashChecker::writeCrashFile($server, $crashData);
                    } else {
                        CloudLogger::get()->info("It tokes too long to stop the server §e" . $server->getName() . "§r! Force shutdown...");
                        if ($server->getTemplate()->getTemplateType() === TemplateType::PROXY()) Utils::copyFile($server->getPath() . "logs/server.log", $server->getTemplate()->getPath() . "logs/server.log");
                        else Utils::copyFile($server->getPath() . "server.log", $server->getTemplate()->getPath() . "server.log");
                    }
                    if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
                    Utils::kill($server->getCloudServerData()->getProcessId());
                }
            } else if ($server->getServerStatus() === ServerStatus::OFFLINE()) {
                CloudServerManager::getInstance()->removeServer($server);
                ServerClientManager::getInstance()->removeClient($server);

                if (CrashChecker::checkCrashed($server, $crashData)) {
                    CloudLogger::get()->info("The server §e" . $server->getName() . " §rwas §ccrashed§r! Creating crashlog...");
                    (new ServerCrashEvent($server, $crashData))->call();
                    CrashChecker::writeCrashFile($server, $crashData);
                    Notifier::sendNotify(NotifyMessage::SERVER_CRASHED()->withReplacements(["server" => $server->getName()]));
                }

                if (!$server->getTemplate()->isStatic()) Utils::deleteDir($server->getPath());
            }
        }
    }
}