<?php

namespace pocketcloud\cloud\console\screen\impl;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\output\impl\ServerConsoleOutputHandler;
use pocketcloud\cloud\console\screen\Screen;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\util\ServerLogStream;
use pocketcloud\cloud\util\TerminalUtils;
use Throwable;

final class ServerConsoleMonitorScreen extends Screen {

    private ?ILogger $logger = null;
    private ?ServerLogStream $stream = null;

    private bool $justStopped = false;
    private ?string $lastInfoMessage = "";
    private int $nextOpenStreamTry = 0;

    public function __construct(private readonly string $serverName) {}

    private function openLogStream(): void {
        $this->stream?->stopStream();
        $server = CloudServerManager::getInstance()->get($this->serverName);
        if ($server !== null) {
            $this->stream = $server->openLogStream();
            try {
                $this->stream->startStream();
            } catch (Throwable $e) {
                $this->printInfoMessage("§8[§c!§8] §cFailed to open log stream§8: §e{}§r, trying again in 3 seconds...", $e->getMessage());
                $this->nextOpenStreamTry = PocketCloud::getInstance()->getTick() + (20 * 3);
                $this->stream = null;
            }
        } else {
            $this->printInfoMessage("§8[§c!§8] §rThe server §b{} §rwas not found. Press §bCTRL + C §rto §ccancel§r.", $this->serverName);
        }
    }

    private function checkLogStream(): void {
        $server = CloudServerManager::getInstance()->get($this->serverName);
        if ($server === null) {
            if ($this->stream === null) {
                if (!$this->justStopped) $this->printInfoMessage("§8[§c!§8] §rThe server §b{} §rwas not found. Press §bCTRL + C §rto §ccancel§r.", $this->serverName);
            } else {
                $this->printInfoMessage("§8[§c!§8] §rThe server §b{} §rhas been stopped. Press §bCTRL + C §rto §ccancel §ror continue waiting.", $this->serverName);
                $this->stream->stopStream();
                $this->stream = null;
                $this->justStopped = true;
            }
        } else {
            if ($this->stream === null && PocketCloud::getInstance()->getTick() >= $this->nextOpenStreamTry) {
                $this->justStopped = false;
                $this->printInfoMessage("§8[§c!§8] §rThe server §b{} §rhas been §astarted§r. Starting log stream...", $this->serverName);
                $this->openLogStream();
            }
        }
    }

    public function initialize(Console $console): void {
        $this->clearConsole();
        $this->setControlCHandler(fn() => ScreenManager::getInstance()->resetScreen());
        $this->setCompletionHandler(fn() => []);

        $this->logger = CloudLogger::tmp();
        $this->logger->setFormat("§r{message}");

        $this->setPrompt("§c" . TerminalUtils::getCurrentUser() . "§8@§b" . $this->serverName. " §7» §r");

        $this->setOutputHandler($outputHandler = new ServerConsoleOutputHandler());
        $outputHandler->addAuthorizedLogger($this->logger);

        $this->openLogStream();
    }

    public function handleInput(string $input): void {
        if (trim($input) === "") return;
        $server = CloudServerManager::getInstance()->get($this->serverName);
        if ($server !== null) {
            $server->executeCommand($input);
        } else {
            $this->printInfoMessage("§8[§c!§8] §rThe server §b{} §rwas not found. Press §bCTRL + C §rto §ccancel§r.", $this->serverName);
        }
    }

    public function tick(int $currentTick): void {
        $this->checkLogStream();
        if ($this->stream !== null) {
            while (true) {
                $line = $this->stream->readNewLine();
                if ($line === null || $line === false) break;
                $this->printLog($line);
            }
        }
    }

    public function onRemove(int $currentTick): void {
        $this->stream?->stopStream();
        $this->stream = null;

        $this->clearConsole();
        $this->enableHistory();
        $this->resetOutputManager();
        $this->setInput("");
        $this->restoreAll();
        $this->printLogCache();
    }

    private function printInfoMessage(string $message, string... $params): void {
        if ($this->lastInfoMessage === $message) return;
        $this->logger->info($message, ...$params);
        $this->lastInfoMessage = $message;
    }

    private function printLog(string $message): void {
        $this->logger->info($message);
        $this->lastInfoMessage = null;
    }
}