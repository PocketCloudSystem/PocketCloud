<?php

namespace pocketcloud\cloud\console\screen\impl;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\output\SetupOutputHandler;
use pocketcloud\cloud\console\screen\IScreen;
use pocketcloud\cloud\setup\Setup;

final class SetupScreen extends IScreen {

    private ?SetupOutputHandler $outputHandler = null;
    private ?ILogger $logger = null;

    public function __construct(private readonly Setup $setup) {}

    public function initialize(Console $console): void {
        $this->clearConsole();
        $this->disableHistory();
        $this->setControlCHandler(fn() => $this->setup->cancel());
        $this->setCompletionHandler(function (array $tokens, string $current): array {
            $recommendations = $this->setup->getCurrentQuestion()?->getPossibleAnswers() ?? [];
            if (empty($recommendations) || !empty($tokens)) return [];
            $matches = [];
            foreach ($recommendations as $recommendation) {
                if (str_starts_with(strtolower($recommendation), strtolower($current))) {
                    $matches[] = $recommendation;
                }
            }

            return $matches;
        });

        $this->logger = CloudLogger::tmp();
        $this->logger->setFormat("§r{message}");

        $this->setOutputHandler($this->outputHandler = new SetupOutputHandler());
        $this->outputHandler->addAuthorizedLogger($this->logger);
    }

    public function tick(int $currentTick): void {}

    public function handleInput(string $input): void {
        $this->setup->handleInput($input);
    }

    public function onRemove(int $currentTick): void {
        $this->clearConsole();
        $this->enableHistory();
        $this->resetOutputManager();
        $this->setInput("");
        $this->restoreAll();
        $this->printLogCache();
    }

    public function getOutputHandler(): ?SetupOutputHandler {
        return $this->outputHandler;
    }

    public function getLogger(): ?ILogger {
        return $this->logger;
    }
}