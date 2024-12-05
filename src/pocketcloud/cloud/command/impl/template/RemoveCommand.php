<?php

namespace pocketcloud\cloud\command\impl\template;

use pocketcloud\cloud\command\argument\def\TemplateArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\template\TemplateManager;

class RemoveCommand extends Command {

    public function __construct() {
        parent::__construct("remove", "Remove a template");
        $this->addParameter(new TemplateArgument(
            "template",
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        TemplateManager::getInstance()->remove($args["template"]);
        return true;
    }
}