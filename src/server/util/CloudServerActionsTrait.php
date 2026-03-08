<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\server\ServerCrashEvent;
use pocketcloud\cloud\event\impl\server\ServerDisconnectEvent;
use pocketcloud\cloud\event\impl\server\ServerStartFailedEvent;
use pocketcloud\cloud\event\impl\server\ServerStopTimeOutEvent;
use pocketcloud\cloud\event\impl\server\ServerTimeOutEvent;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\data\NotificationType;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\network\packet\impl\ProxyRegisterServerPacket;
use pocketcloud\cloud\network\packet\impl\ProxyUnregisterServerPacket;
use pocketcloud\cloud\network\packet\impl\request\client\CommandExecuteRequestPacket;
use pocketcloud\cloud\network\packet\impl\response\client\CommandExecuteResponsePacket;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\crash\CrashChecker;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\promise\Promise;

trait CloudServerActionsTrait {

    /** @var array<array{Promise, int}> */
    private array $commandExecutionOrders = [];

    /** @deprecated */
    public function tickCommandOrders(): void {
        foreach ($this->commandExecutionOrders as $id => $order) {
            if (($order[1] + 5) <= time()) {
                $this->handleFailedCommandResponse($id);
            }
        }
    }

    public function printCrashStackTrace(array $crashData): void {
        CloudLogger::get()->info("§8[§cERROR§8/§e{}§r§8] §cUnhandled §e{}§c: §e{} §cwas thrown in §e{} §cat line §e{}", $this->getName(), $crashData["error"]["type"], $crashData["error"]["message"] ?? "Unknown error", $crashData["error"]["file"], $crashData["error"]["line"]);
        foreach ($crashData["trace"] as $message) CloudLogger::get()->error("§c" . $message);
    }

    /**
     * @param string $commandLine
     * @return Promise<ServerCommandExecutionResult>
     */
    public function executeCommand(string $commandLine): Promise {
        $promise = new Promise();
        if (($client = $this->getServerClient()) === null) return Promise::rejected("Not verified yet");
        if (!($reqPacket = CommandExecuteRequestPacket::create($commandLine, $id = uniqid("command-")))->sendRequest($client)) return Promise::rejected("Failed to send packet");
        $reqPacket->then(fn(CommandExecuteResponsePacket $packet) => $this->handleCommandResponse($packet->getCommandExecutionResult()))
            ->failure(fn() => $this->handleFailedCommandResponse($id));

        $this->commandExecutionOrders[$id] = [$promise, time()];
        return $promise;
    }

    public function handleCommandResponse(ServerCommandExecutionResult $result): void {
        if (isset($this->commandExecutionOrders[$result->getId()])) {
            $this->commandExecutionOrders[$result->getId()][0]->resolve($result);
            unset($this->commandExecutionOrders[$result->getId()]);
        }
    }

    public function handleFailedCommandResponse(string $id): void {
        if (isset($this->commandExecutionOrders[$id])) {
            $this->commandExecutionOrders[$id][0]->reject("Request timeout");
            unset($this->commandExecutionOrders[$id]);
        }
    }

    public function handleFailedStart(): void {
        if ($this->getServerStatus() !== ServerStatus::STARTING) return;
        $this->setServerStatus(ServerStatus::OFFLINE);
        $this->remove();
        $this->killProcess();
        new ServerStartFailedEvent($this)->call();

        if (!$this->checkForCrash()) CloudLogger::get()->warn("Failed to start the server §b{}§r, deleting data...", $this->getName());
        NotificationType::SERVER_START_FAILED->notify(["server" => $this->getName()]);

        $this->deleteTmpDir();
    }

    public function handleTimeout(): void {
        if (!$this->getServerStatus()?->isOnline()) return;
        $this->setServerStatus(ServerStatus::OFFLINE);
        $this->remove();
        $this->killProcess();
        new ServerTimeoutEvent($this)->call();

        if (!$this->checkForCrash()) CloudLogger::get()->warn("The server §b{} §r§ctimed out§r, deleting data...", $this->getName());
        NotificationType::SERVER_TIMED_OUT->notify(["server" => $this->getName()]);

        $this->deleteTmpDir();
    }

    public function handleStopTimeout(): void {
        if ($this->getServerStatus() !== ServerStatus::STOPPING) return;
        $this->setServerStatus(ServerStatus::OFFLINE);
        $this->remove();
        $this->killProcess();
        new ServerStopTimeOutEvent($this)->call();

        if (!$this->checkForCrash()) CloudLogger::get()->warn("Failed to stop the server §b{}§r, deleting data & killing process...", $this->getName());
        NotificationType::SERVER_STOP_TIMED_OUT->notify(["server" => $this->getName()]);

        $this->deleteTmpDir();
    }
    
    public function handleDisconnect(): void {
        if ($this->getServerStatus() === ServerStatus::OFFLINE) {
            CloudServerManager::getInstance()->remove($this);
            return;
        }

        $this->setServerStatus(ServerStatus::OFFLINE);
        new ServerDisconnectEvent($this)->call();
        if (!$this->checkForCrash()) CloudLogger::get()->success("The server §b{} §rhas §cdisconnected §rfrom the cloud.", $this->getName());
        else $this->killProcess();

        $this->remove();
        $this->deleteTmpDir();
    }

    public function saveFiles(): void {
        foreach ($this->getTemplate()->getTemplateType()->getSavableFiles() as $file) {
            $filePath = $this->getPath() . $file;
            $destinationPath = $this->getTemplate()->getPath() . $file;
            if (is_file($filePath)) FileUtils::copyfile($filePath, $destinationPath);
            else FileUtils::copyDirectory($filePath, $destinationPath);
        }
    }

    public function addToProxies(): void {
        if ($this->getTemplate()->getTemplateType()->isServer()) {
            foreach (ServerClientCache::getInstance()->getAll(...TemplateType::onlyProxy()) as $client) {
                ProxyRegisterServerPacket::create($this->getName(), $this->getServerData()->getPort())->sendPacket($client);
            }
        }
    }

    public function removeFromProxies(): void {
        if ($this->getTemplate()->getTemplateType()->isServer()) {
            foreach (ServerClientCache::getInstance()->getAll(...TemplateType::onlyProxy()) as $client) {
                ProxyUnregisterServerPacket::create($this->getName())->sendPacket($client);
            }
        }
    }

    public function remove(): void {
        CloudServerManager::getInstance()->remove($this);
        ServerClientCache::getInstance()->remove($this);

        if ($this->getTemplate()->getTemplateType() === TemplateType::SERVER()) {
            if ($this->getProperties()->get("auto-save") === true) $this->saveFiles();
            $this->removeFromProxies();
        }
    }

    public function killProcess(): void {
        if ($this->getServerData()->getProcessId() !== null) ProcessUtils::kill($this->getServerData()->getProcessId());
    }

    public function checkForCrash(): bool {
        if (CrashChecker::checkCrashed($this, $crashData)) {
            CloudLogger::get()->warn("The server §b{} §ccrashed§r, writing crash file...", $this->getName());
            $this->printCrashStackTrace($crashData);
            new ServerCrashEvent($this, $crashData)->call();
            CrashChecker::writeCrashFile($this, $crashData);
            NotificationType::SERVER_CRASHED->notify(["server" => $this->getName()], ["crashData" => $crashData]);
            return true;
        }

        return false;
    }

    public function saveAndDeleteLogFiles(): void {
        $logFileLocation = $this->getPath() . $this->getTemplate()->getTemplateType()->getRelativeLogFileLocation();
        if (file_exists($logFileLocation)) {
            if (!@is_dir($logArchivePath = PathUtils::join($this->getTemplate()->getPath(), "cloud_log_archive"))) @mkdir($logArchivePath, 0777, true);
            FileUtils::copyFile($logFileLocation, PathUtils::join($logArchivePath, date("Y-m-d_H:i:s.v_e", (int) floor($this->startTime)) . "_" . basename($logFileLocation) . ".log"));
            @unlink($logFileLocation);
        }
    }

    public function deleteTmpDir(): void {
        $this->saveAndDeleteLogFiles();
        if (!$this->getTemplate()?->isStatic()) {
            CloudLogger::get()->debug("Removed server directory: {}", $this->getPath());
            FileUtils::removeDirectory($this->getPath());
        }
    }
}