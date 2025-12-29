<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;

readonly class TemplateArgument extends CommandArgument {

    public function parseValue(string $input): Template {
        if (($template = TemplateManager::getInstance()->get($input)) !== null) return $template;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(TemplateManager::getInstance()->getAll()), fn(string $template) => str_contains(strtolower($template), $currentArg));
    }

    public function getType(): string {
        return "server";
    }
}