<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;

final readonly class TemplateArgument extends CommandArgument {

    public function parseValue(string $input): Template {
        if (($template = TemplateManager::getInstance()->get($input)) !== null) return $template;
        throw new ArgumentParseException();
    }

    public function getType(): string {
        return "template";
    }
}