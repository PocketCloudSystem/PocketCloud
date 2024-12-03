<?php

namespace pocketcloud\cloud\template;

use pocketcloud\cloud\util\Utils;

final class TemplateHelper {

    public const KEYS = ["name", "lobby", "maintenance", "static", "maxPlayerCount", "minServerCount", "maxServerCount", "startNewWhenFull", "autoStart", "templateType"];
    public const EDITABLE_KEYS = ["lobby", "maintenance", "static", "maxPlayerCount", "minServerCount", "maxServerCount", "startNewWhenFull", "autoStart"];

    public const NECESSARY_KEYS = ["name", "lobby", "templateType"];

    public const UNNECESSARY_KEYS = ["maintenance", "static", "maxPlayerCount", "minServerCount", "maxServerCount", "startNewWhenFull", "autoStart"];
    public const DEFAULT_VALUES = ["maintenance" => true, "static" => false, "mayPlayerCount" => 20, "minServerCount" => 0, "maxServerCount" => 2, "startNewWhenFull" => true, "autoStart" => true];

    public static function addUnnecessaryKeys(array $data): void {
        foreach (array_filter(self::UNNECESSARY_KEYS, fn(string $key) => !isset($data[$key])) as $key) $data[$key] = self::DEFAULT_VALUES[$key];
    }

    public static function sumSettingsToInstance(array $data): ?TemplateSettings {
        if (Utils::containKeys($data, ...self::EDITABLE_KEYS)) {
            $onlySettings = [];
            foreach (self::EDITABLE_KEYS as $key) $onlySettings[$key] = $data[$key];
            return TemplateSettings::fromArray($onlySettings);
        }
        return null;
    }

    public static function isValidEditValue(string $value, string $key, ?string &$expected = null, mixed &$realValue = null): bool {
        if ($key == "lobby" || $key == "maintenance" || $key == "autoStart" || $key == "static" || $key == "startNewWhenFull") {
            $expected = "true | false";
            if ($value == "true" || $value == "false") {
                $realValue = $value == "true";
                return true;
            }
        } else if ($key == "maxPlayerCount" || $key == "minServerCount" || $key == "maxServerCount") {
            $expected = "number";
            if (is_numeric($value)) {
                $realValue = max(intval($value), 0);
                return true;
            }
        }
        return false;
    }

    public static function isValidEditKey(string $key): bool {
        return in_array($key, self::EDITABLE_KEYS);
    }
}