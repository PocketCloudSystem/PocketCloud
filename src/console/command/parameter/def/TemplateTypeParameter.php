<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\template\TemplateType;

readonly class TemplateTypeParameter extends CommandParameter {

    public function parseValue(string $input): TemplateType {
        if (($templateType = TemplateType::get($input)) !== null) return $templateType;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(TemplateType::getAll()), fn(string $template) => str_contains(strtolower($template), $currentArg));
    }

    public function getType(): string {
        return "template_type";
    }
}