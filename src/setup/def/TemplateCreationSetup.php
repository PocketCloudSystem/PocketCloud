<?php

namespace pocketcloud\cloud\setup\def;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\setup\QuestionBuilder;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;

final class TemplateCreationSetup extends Setup {

    public function onStart(ILogger $logger): void {
        $this->setPrefix("§bTemplate-Setup");
        $logger->info("Welcome to the Template-Setup!");
    }

    public function onCancel(): void {
        CloudLogger::get()->warn("The template setup was cancelled!");
    }

    public function applyQuestions(): array {
        return [
            QuestionBuilder::builder("name", "What's the name of your template?")
                ->parser(function(string $input, ?string &$error): ?string {
                    if (TemplateManager::getInstance()->check($input)) {
                        $error = "A template with that name already eixsts!";
                        return null;
                    }

                    return $input;
                })
                ->build(),
            QuestionBuilder::builder("lobby", "Is your template a lobby?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default("No", false)
                ->build(),
            QuestionBuilder::builder("maintenance", "Should your template be in maintenance?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default("Yes", true)
                ->build(),
            QuestionBuilder::builder("static", "Should your template be static, meaning the servers of that template just have their own data?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default("No", false)
                ->build(),
            QuestionBuilder::builder("alwaysCopyToStaticServers", "Should your static servers always copy data from their template?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->default("No", false)
                ->build(),
            QuestionBuilder::builder("autoStart", "Should your template start servers automatically?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
                ->default("Yes", true)
                ->build(),
            QuestionBuilder::builder("startNewPercentage", "How many players are required to start a new server? (in %, 0-100, 0 = none)")
                ->parser(function(string $input): ?string {
                    if (is_numeric($input) && ($val = floatval($input)) >= 0 && $val <= 100) return floatval($input / 100);
                    return null;
                })
                ->canSkipped(true)
                ->recommendation("75%")
                ->default("0% -> Disabled", 0)
                ->build(),
            QuestionBuilder::builder("maxPlayerCount", "How many players are allowed on that template's servers?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->default("20 players", 20)
                ->canSkipped(true)
                ->build(),
            QuestionBuilder::builder("minServerCount", "How many servers should always be running?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->default("1 server", 1)
                ->canSkipped(true)
                ->build(),
            QuestionBuilder::builder("maxServerCount", "How many servers can be running in total?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->default("2 servers", 2)
                ->canSkipped(true)
                ->build(),
            QuestionBuilder::builder("templateType", "What type of template is your template?")
                ->parser(function (string $input, ?string &$error): ?TemplateType {
                    if (($templateType = TemplateType::get($input)) === null) {
                        $error = "No template type found with that name!";
                        return null;
                    }

                    return $templateType;
                })
                ->canSkipped(false)
                ->possibleAnswers(...array_keys(TemplateType::getAll()))
                ->build()
        ];
    }

    public function handleResults(array $results): void {
        TemplateManager::getInstance()->create(new Template(
            $results["name"],
            TemplateHelper::sumSettingsToInstance($results),
            $results["templateType"]
        ));
    }
}