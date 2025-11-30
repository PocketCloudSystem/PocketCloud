<?php

namespace pocketcloud\cloud\command\impl\template;

use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\setup\impl\TemplateSetup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateSettings;
use pocketcloud\cloud\template\TemplateType;

final class CreateCommand extends Command {

    public function __construct() {
        parent::__construct("create", "Create a template");
        $this->addParameter(new StringArgument(
            "name",
            true
        ));

        $this->addParameter(new StringEnumArgument(
            "type",
            ["server", "proxy"],
            false,
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $name = $args["name"] ?? null;
        if ($name === null) {
            new TemplateSetup()->startSetup();
            return true;
        } else {
            if (!TemplateManager::getInstance()->check($name)) {
                $templateType = TemplateType::SERVER();
                if (isset($args["type"])) $templateType = TemplateType::get($args["type"]) ?? TemplateType::SERVER();

                TemplateManager::getInstance()->create(Template::create($name, TemplateSettings::create(false, true, false, 20, 0, 2, 100, false), $templateType));
            } else $sender->error("The template already exists!");
        }
        return true;
    }
}