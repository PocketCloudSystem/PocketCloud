<?php

namespace pocketcloud\cloud\setup\impl;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\exception\ExceptionHandler;
use pocketcloud\cloud\server\util\ServerUtils;
use pocketcloud\cloud\setup\QuestionBuilder;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\terminal\log\logger\Logger;

final class ConfigSetup extends Setup {

    public function onStart(Logger $logger): void {
        $logger->info("Welcome to the §bPocket§3Cloud§r-Setup!");
    }

    public function onCancel(): void {
        CloudLogger::get()->warn("The config setup was cancelled!");
    }

    public function applyQuestions(): array {
        return [
            QuestionBuilder::builder()
                ->key("networkPort")
                ->question("Which port should the cloud use?")
                ->canSkipped(true)
                ->default(3656)
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
            ->build(),
            QuestionBuilder::builder()
                ->key("memoryLimit")
                ->question("How much memory should be available for the cloud?")
                ->recommendation("512")
                ->default("512")
                ->canSkipped(true)
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
            ->build(),
            QuestionBuilder::builder()
                ->key("debugMode")
                ->question("Do you want to enable the debug mode?")
                ->canSkipped(true)
                ->default("yes")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("updateChecks")
                ->question("Should the cloud check for updates by itself?")
                ->canSkipped(true)
                ->default("yes")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("executeUpdates")
                ->question("Should the cloud execute those updates?")
                ->canSkipped(true)
                ->default("yes")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
                ->build(),
            QuestionBuilder::builder()
                ->key("startMethod")
                ->question("What type of start method do you want to use?")
                ->canSkipped(true)
                ->default((ServerUtils::checkTmux() ? "tmux" : "screen"))
                ->parser(fn(string $input) => $input)
                ->possibleAnswers("tmux", "screen")
                ->recommendation((ServerUtils::checkTmux() ? "tmux" : "screen"))
            ->build(),
            QuestionBuilder::builder()
                ->key("httpServerEnabled")
                ->question("Do you want to enable the HTTP server?")
                ->canSkipped(true)
                ->default("yes")
                ->parser(fn(string $input) => strtolower($input) == "true")
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("httpServerPort")
                ->question("Which port should be used by the http server?")
                ->canSkipped(true)
                ->default("8000")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
            ->build(),
            QuestionBuilder::builder()
                ->key("defaultLobbyTemplate")
                ->question("Do you want to create a default Lobby template?")
                ->canSkipped(true)
                ->parser(fn(string $input) => strtolower($input) == "true")
                ->default("yes")
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("defaultProxyTemplate")
                ->question("Do you want to create a default Proxy template?")
                ->canSkipped(true)
                ->parser(fn(string $input) => strtolower($input) == "true")
                ->default("yes")
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build()
        ];
    }

    public function handleResults(array $results): void {
        MainConfig::getInstance()->setNetworkPort($results["networkPort"] ?? 3656);
        MainConfig::getInstance()->setMemoryLimit($results["memoryLimit"] ?? 512);
        MainConfig::getInstance()->setDebugMode($results["debugMode"] ?? true);
        MainConfig::getInstance()->setStartMethod($results["startMethod"] ?? (ServerUtils::checkTmux() ? "tmux" : "screen"));
        MainConfig::getInstance()->setHttpServerEnabled($results["httpServerEnabled"] ?? true);
        MainConfig::getInstance()->setHttpServerPort($results["httpServerPort"] ?? 8000);
        ExceptionHandler::tryCatch(fn() => MainConfig::getInstance()->save(), "Failed to save main config");
    }
}