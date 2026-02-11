<?php

namespace pocketcloud\cloud\setup\def;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\language\Language;
use pocketcloud\cloud\setup\QuestionBuilder;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\util\FormatUtils;

final class ConfigSetup extends Setup {

    public function onStart(ILogger $logger): void {
        $this->setPrefix("§bConfiguration-Setup");
        $logger->info("Welcome to the Configuration Setup!");
    }

    public function onCancel(): void {
        CloudLogger::get()->warn("The configuration setup was cancelled!");
    }

    public function applyQuestions(): array {
        return [
            QuestionBuilder::builder("memoryLimit", "What's the memory limit for the cloud? (in MB)")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input) || intval($input) <= 0) return null;
                    return intval($input);
                })
                ->default(FormatUtils::bytes(MainConfig::getInstance()->getMemoryLimit()), MainConfig::getInstance()->getMemoryLimit())
                ->canSkipped(true)
                ->build(),
            QuestionBuilder::builder("language", "What language should the cloud use?")
                ->parser(fn(string $input) => $input)
                ->canSkipped(true)
                ->possibleAnswers(...array_map(fn(Language $language) => $language->getAliases()[0], Language::getAll()))
                ->default(Language::current()->getAliases()[0], Language::current()->getAliases()[0])
                ->build(),
            QuestionBuilder::builder("updateChecks", "Should the cloud check for updates?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isUpdateChecks() ? "Yes" : "No", MainConfig::getInstance()->isUpdateChecks())
                ->build(),
            QuestionBuilder::builder("executeUpdates", "Should updates be executed automatically?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isExecuteUpdates() ? "Yes" : "No", MainConfig::getInstance()->isExecuteUpdates())
                ->build(),
            QuestionBuilder::builder("startUpDelay", "Should there be a startup delay?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isStartUpDelay() ? "Yes" : "No", MainConfig::getInstance()->isStartUpDelay())
                ->build(),
            QuestionBuilder::builder("writeTimingsOnShutdown", "Should timings be written on shutdown?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isWriteTimingsOnShutdown() ? "Yes" : "No", MainConfig::getInstance()->isWriteTimingsOnShutdown())
                ->build(),
            QuestionBuilder::builder("bStatsEnabled", "Should bStats be enabled?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isBStatsEnabled() ? "Yes" : "No", MainConfig::getInstance()->isBStatsEnabled())
                ->build(),
            QuestionBuilder::builder("bStatsLogFailedRequests", "Should bStats log failed requests?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isBStatsLogFailedRequests() ? "Yes" : "No", MainConfig::getInstance()->isBStatsLogFailedRequests())
                ->build(),
            QuestionBuilder::builder("bStatsLogSentData", "Should bStats log sent data?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isBStatsLogSentData() ? "Yes" : "No", MainConfig::getInstance()->isBStatsLogSentData())
                ->build(),
            QuestionBuilder::builder("bStatsLogResponseStatus", "Should bStats log response status text?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isBStatsLogResponseStatusText() ? "Yes" : "No", MainConfig::getInstance()->isBStatsLogResponseStatusText())
                ->build(),
            QuestionBuilder::builder("networkAddress", "What's the network address?")
                ->parser(fn(string $input) => $input)
                ->canSkipped(true)
                ->default(MainConfig::getInstance()->getNetworkAddress(), MainConfig::getInstance()->getNetworkAddress())
                ->build(),
            QuestionBuilder::builder("networkPort", "What's the network port?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input) || intval($input) <= 0 || intval($input) > 65535) return null;
                    return intval($input);
                })
                ->canSkipped(true)
                ->default((string) MainConfig::getInstance()->getNetworkPort(), MainConfig::getInstance()->getNetworkPort())
                ->build(),
            QuestionBuilder::builder("networkEncryption", "Should network encryption be enabled?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isNetworkEncryptionEnabled() ? "Yes" : "No", MainConfig::getInstance()->isNetworkEncryptionEnabled())
                ->build(),
            QuestionBuilder::builder("networkOnlyLocal", "Should network connections be limited to local only?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default(MainConfig::getInstance()->isNetworkOnlyLocal() ? "Yes" : "No", MainConfig::getInstance()->isNetworkOnlyLocal())
                ->build()
        ];
    }

    public function handleResults(array $results): void {
        $config = [
            "memoryLimit" => $results["memoryLimit"],
            "language" => $results["language"],
            "updateChecks" => $results["updateChecks"],
            "executeUpdates" => $results["executeUpdates"],
            "startUpDelay" => $results["startUpDelay"],
            "writeTimingsOnShutdown" => $results["writeTimingsOnShutdown"],
            "bStats" => [
                "enabled" => $results["bStatsEnabled"],
                "log_failed_requests" => $results["bStatsLogFailedRequests"],
                "log_sent_data" => $results["bStatsLogSentData"],
                "log_response_status_text" => $results["bStatsLogResponseStatus"]
            ],
            "network" => [
                "address" => $results["networkAddress"],
                "port" => $results["networkPort"],
                "encryption" => $results["networkEncryption"],
                "only-local" => $results["networkOnlyLocal"]
            ]
        ];

        MainConfig::getInstance()->setMemoryLimit($config["memoryLimit"]);
        MainConfig::getInstance()->setLanguage($config["language"]);
        MainConfig::getInstance()->setUpdateChecks($config["updateChecks"]);
        MainConfig::getInstance()->setExecuteUpdates($config["executeUpdates"]);
        MainConfig::getInstance()->setStartUpDelay($config["startUpDelay"]);
        MainConfig::getInstance()->setWriteTimingsOnShutdown($config["writeTimingsOnShutdown"]);
        MainConfig::getInstance()->setBStats($config["bStats"]);
        MainConfig::getInstance()->setNetwork($config["network"]);
        ExceptionHandler::tryCatch(function (): void {
            MainConfig::getInstance()->save();
            CloudLogger::get()->success("Your configuration has been §asaved§r. Restart the cloud mto apply the changes made.");
        }, "Failed to save configuration");
    }
}