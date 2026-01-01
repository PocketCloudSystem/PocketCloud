<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\impl\server\ServerCrashEvent;
use pocketcloud\cloud\event\impl\server\ServerDisconnectEvent;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\packet\data\NotificationType;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\network\packet\impl\CommandExecutePacket;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\crash\CrashChecker;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\TerminalUtils;

trait CloudServerActionsTrait {

    /** @var array<array{Promise, int}> */
    private array $commandExecutionOrders = [];

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
        if (!CommandExecutePacket::create($commandLine, $id = uniqid("command-"))->sendPacket($client)) return Promise::rejected("Failed to send packet");
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
    
    public function handleDisconnect(): void {
        if ($this->getServerStatus() === ServerStatus::OFFLINE) {
            CloudServerManager::getInstance()->remove($this);
            return;
        }

        $this->setServerStatus(ServerStatus::OFFLINE);
        new ServerDisconnectEvent($this)->call();
        if (!$this->checkForCrash()) CloudLogger::get()->success("The server §b{} §rhas §cdisconnected §rfrom the cloud.", $this->getName());

        $this->killProcess();
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

    public function remove(): void {
        CloudServerManager::getInstance()->remove($this);
        ServerClientCache::getInstance()->remove($this);
    }

    public function killProcess(): void {
        if ($this->getCloudServerData()->getProcessId() !== 0) TerminalUtils::kill($this->getCloudServerData()->getProcessId());
    }

    public function checkForCrash(): bool {
        if (CrashChecker::checkCrashed($this, $crashData)) {
            CloudLogger::get()->warn("The server §b{} §ccrashed§r, writing crash file...", $this->getName());
            $this->printCrashStackTrace($crashData);
            new ServerCrashEvent($this, $crashData)->call();
            CrashChecker::writeCrashFile($this, $crashData);
            NotificationType::SERVER_CRASHED->notify(["%server%" => $this->getName()]);
            return true;
        }

        return false;
    }

    public function deleteTmpDir(): void {
        if (!$this->getTemplate()->getSettings()->isStatic()) FileUtils::removeDirectory($this->getPath());
    }
}