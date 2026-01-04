<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;

readonly class TemplateParameter extends CommandParameter {

    public function parseValue(string $input): Template {
        if (($template = TemplateManager::getInstance()->get($input)) !== null) return $template;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(TemplateManager::getInstance()->getAll()), fn(string $template) => str_contains(strtolower($template), $currentArg));
    }

    public function getType(): string {
        return "template";
    }
}