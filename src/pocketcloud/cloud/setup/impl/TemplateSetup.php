<?php

namespace pocketcloud\cloud\setup\impl;

use pocketcloud\cloud\setup\QuestionBuilder;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\terminal\log\logger\Logger;

final class TemplateSetup extends Setup {

    public function onStart(Logger $logger): void {
        $this->setPrefix("§bTemplate-Setup");
        $logger->info("Welcome to the Template-Setup!");
    }

    public function onCancel(): void {
        CloudLogger::get()->warn("The template setup was cancelled!");
    }

    public function applyQuestions(): array {
        return [
            QuestionBuilder::builder()
                ->key("name")
                ->question("What's the name of your template?")
                ->parser(function(string $input): ?string {
                    if (TemplateManager::getInstance()->check($input)) return null;
                    return $input;
                })
            ->build(),
            QuestionBuilder::builder()
                ->key("lobby")
                ->question("Is your template a lobby?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build(),
            QuestionBuilder::builder()
                ->key("maintenance")
                ->question("Should your template be in maintenance?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build(),
            QuestionBuilder::builder()
                ->key("static")
                ->question("Should your template be static?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build(),
            QuestionBuilder::builder()
                ->key("autoStart")
                ->question("Should your template start servers automatically?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("startNewPercentage")
                ->question("How many players are required to start a new server? (in %, 1-100)")
                ->parser(function(string $input): ?string {
                    if (is_numeric($input) && ($val = floatval($input)) > 0 && $val < 100) return floatval($input);
                    return null;
                })
                ->canSkipped(true)
                ->recommendation("75%")
            ->build(),
            QuestionBuilder::builder()
                ->key("maxPlayerCount")
                ->question("How many players are allowed on that template?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->canSkipped(true)
            ->build(),
            QuestionBuilder::builder()
                ->key("minServerCount")
                ->question("How many servers should always be online?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->canSkipped(true)
            ->build(),
            QuestionBuilder::builder()
                ->key("maxServerCount")
                ->question("How many server can be online?")
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->canSkipped(true)
            ->build(),
            QuestionBuilder::builder()
                ->key("templateType")
                ->question("Is your template a proxy?")
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build()
        ];
    }

    public function handleResults(array $results): void {
        TemplateManager::getInstance()->create(new Template(
            $results["name"],
            TemplateHelper::sumSettingsToInstance($results),
            ($results["templateType"] ?? false) ? TemplateType::PROXY() : TemplateType::SERVER()
        ));
    }
}