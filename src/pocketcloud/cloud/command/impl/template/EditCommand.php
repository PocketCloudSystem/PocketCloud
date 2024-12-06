<?php

namespace pocketcloud\cloud\command\impl\template;

use pocketcloud\cloud\command\argument\def\MixedArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\argument\def\TemplateArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\template\TemplateManager;

class EditCommand extends Command {

    public function __construct() {
        parent::__construct("edit", "Edit a template");

        $this->addParameter(new TemplateArgument(
            "template",
            false,
            "The template was not found."
        ));

        $this->addParameter(new StringEnumArgument(
            "key",
            TemplateHelper::EDITABLE_KEYS,
            true,
            true,
            "The key you are trying to edit was not found."
        ));

        $this->addParameter(new MixedArgument(
            "value",
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $template = $args["template"];
        $key = $args["key"];
        $value = $args["value"];

        if (TemplateHelper::isValidEditKey($key)) {
            if (TemplateHelper::isValidEditValue($value, $key, $expected, $realValue)) {
                TemplateManager::getInstance()->edit(
                    $template,
                    ($args[1] == "lobby" ? $realValue : null),
                    ($args[1] == "maintenance" ? $realValue : null),
                    ($args[1] == "static" ? $realValue : null),
                    ($args[1] == "maxPlayerCount" ? $realValue : null),
                    ($args[1] == "minServerCount" ? $realValue : null),
                    ($args[1] == "maxServerCount" ? $realValue : null),
                    ($args[1] == "startNewWhenFull" ? $realValue : null),
                    ($args[1] == "autoStart" ? $realValue : null),
                );
            } else $sender->error("Wrong value provided, expected an §b" . $expected . " §rwhen editing §b" . $key . "§r!");
        } else $sender->error("Undefined key provided!");
        return true;
    }
}