<?php

namespace pocketcloud\cloud\console\command\impl\template;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\parameter\def\StringParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\setup\def\TemplateCreationSetup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateSettings;

final class TemplateCommand extends Command {

    public function __construct() {
        parent::__construct("template", "Manage your templates", ["temp"]);

        $this->registerSubCommand(SubCommand::fromClosure("create", $this->handleCreateSub(...))
            ->addParameter(new StringParameter("name", true))
            ->addParameter(new StringParameter("templateType", true)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }

    private function handleCreateSub(ICommandSender $sender, string $label, array $args): bool {
        if (count($args) == 0) {
            TemplateCreationSetup::new()->startSetup();
            return true;
        }

        $name = $args["name"] ?? null;
        $templateType = $args["templateType"] ?? null;
        if ($name === null || $templateType === null) return false;

        if (TemplateManager::getInstance()->check($name)) {
            $sender->error("Template already exists!");
        } else {
            TemplateManager::getInstance()->create(Template::create($name, TemplateSettings::default(), $templateType));
        }

        return true;
    }
}