<?php

namespace pocketcloud\cloud\command\impl\server;

use pocketcloud\cloud\command\argument\def\IntegerArgument;
use pocketcloud\cloud\command\argument\def\MultipleTypesArgument;
use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\argument\def\TemplateArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

class StartCommand extends Command {

    public function __construct() {
        parent::__construct("start", "Start a server");

        $this->addParameter(new TemplateArgument(
            "template",
            false,
            "The template was not found."
        ));

        $this->addParameter(new MultipleTypesArgument(
            "action",
            [
                new IntegerArgument("count", true),
                new StringArgument("template", true, true)
            ],
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $template = $args["template"];
        $count = 1;
        $object = $args["object"] ?? 1;
        if (is_int($object) && $object > 0) {
            $count = $object;
        } else if (is_string($object)) {
            $templates = explode(" ", $object);
            foreach ($templates as $arg) {
                if (($argTemplate = TemplateManager::getInstance()->get($arg)) !== null) CloudServerManager::getInstance()->start($argTemplate, $count);
            }
        }

        CloudServerManager::getInstance()->start($template, $count);
        return true;
    }
}