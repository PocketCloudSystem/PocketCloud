<?php

namespace pocketcloud\setup\impl;

use pocketcloud\language\Language;
use pocketcloud\setup\QuestionBuilder;
use pocketcloud\setup\Setup;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateHelper;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\util\CloudLogger;

class TemplateSetup extends Setup {

    public function onStart(): void {
        $this->getLogger()->info(Language::current()->translate("setup.template.welcome"));
    }

    public function onCancel(): void {
        CloudLogger::get()->warn(Language::current()->translate("setup.template.cancelled"));
    }

    public function applyQuestions(): array {
        return [
            QuestionBuilder::builder()
                ->key("name")
                ->question(Language::current()->translate("setup.template.question.name"))
                ->parser(function(string $input): ?string {
                    if (TemplateManager::getInstance()->checkTemplate($input)) return null;
                    return $input;
                })
            ->build(),
            QuestionBuilder::builder()
                ->key("lobby")
                ->question(Language::current()->translate("setup.template.question.lobby"))
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build(),
            QuestionBuilder::builder()
                ->key("maintenance")
                ->question(Language::current()->translate("setup.template.question.maintenance"))
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build(),
            QuestionBuilder::builder()
                ->key("static")
                ->question(Language::current()->translate("setup.template.question.static"))
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build(),
            QuestionBuilder::builder()
                ->key("autoStart")
                ->question(Language::current()->translate("setup.template.question.auto_start"))
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("startNewWhenFull")
                ->question(Language::current()->translate("setup.template.question.start_new"))
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
                ->recommendation("yes")
            ->build(),
            QuestionBuilder::builder()
                ->key("maxPlayerCount")
                ->question(Language::current()->translate("setup.template.question.max_players"))
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->canSkipped(true)
            ->build(),
            QuestionBuilder::builder()
                ->key("minServerCount")
                ->question(Language::current()->translate("setup.template.question.min_servers"))
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->canSkipped(true)
            ->build(),
            QuestionBuilder::builder()
                ->key("maxServerCount")
                ->question(Language::current()->translate("setup.template.question.max_servers"))
                ->parser(function(string $input): ?int {
                    if (!is_numeric($input)) return null;
                    return intval($input);
                })
                ->canSkipped(true)
            ->build(),
            QuestionBuilder::builder()
                ->key("templateType")
                ->question(Language::current()->translate("setup.template.question.proxy"))
                ->parser(fn(string $input) => strtolower($input) == "yes")
                ->canSkipped(true)
                ->possibleAnswers("yes", "no")
            ->build()
        ];
    }

    public function handleResults(array $results): void {
        TemplateManager::getInstance()->createTemplate(new Template(
            $results["name"],
            TemplateHelper::sumSettingsToInstance($results),
            ($results["templateType"] ?? false) ? TemplateType::PROXY() : TemplateType::SERVER()
        ));
    }
}