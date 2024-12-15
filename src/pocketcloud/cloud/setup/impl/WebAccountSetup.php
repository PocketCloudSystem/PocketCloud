<?php

namespace pocketcloud\cloud\setup\impl;

use pocketcloud\cloud\setup\QuestionBuilder;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\terminal\log\logger\Logger;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\web\WebAccount;
use pocketcloud\cloud\web\WebAccountManager;
use pocketcloud\cloud\web\WebAccountRoles;

final class WebAccountSetup extends Setup {

    public function onStart(Logger $logger): void {
        $this->setPrefix("§cWebAccount-Setup");
        $logger->info("Welcome to the §cWebAccount§r-Setup!");
    }

    public function onCancel(): void {
        CloudLogger::get()->warn("The web-account setup was cancelled!");
    }

    public function applyQuestions(): array {
        return [
            QuestionBuilder::builder()
                ->key("name")
                ->question("What is the name of your web account?")
                ->canSkipped(false)
                ->parser(function(string $input): ?string {
                    if (WebAccountManager::getInstance()->check($input)) return null;
                    return $input;
                })
                ->build(),
            QuestionBuilder::builder()
                ->key("role")
                ->question("Which role should the account be assigned to?")
                ->possibleAnswers("default", "admin")
                ->recommendation("default")
                ->default("default")
                ->canSkipped(true)
                ->parser(function(string $input): ?WebAccountRoles {
                    if (($role = WebAccountRoles::get(strtolower($input))) === null) return null;
                    return $role;
                })
                ->build()
        ];
    }

    public function handleResults(array $results): void {
        WebAccountManager::getInstance()->create(new WebAccount($name = $results["name"], $initPassword = Utils::generateString(6), true, $role = ($results["role"] ?? WebAccountRoles::DEFAULT)));
        CloudLogger::get()->success("Successfully §acreated §rthe web account §b" . $name . " §rwith the role §b" . $role->value . "§r. §8(§rInitial Password: §b" . $initPassword . "§8)");
    }
}