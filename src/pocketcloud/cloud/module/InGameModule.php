<?php

namespace pocketcloud\cloud\module;

final class InGameModule {

    public const SIGN_MODULE = "sign_module";
    public const NPC_MODULE = "npc_module";
    public const HUB_COMMAND_MODULE = "hub_command_module";

    private static array $moduleStates = [];

    public static function setModuleState(string $module, bool $enabled): void {
        if (isset(self::$moduleStates[$module])) self::$moduleStates[$module] = $enabled;
    }

    public static function getModuleState(string $module): bool {
        return self::$moduleStates[$module] ?? false;
    }

    public static function getAll(): array {
        return [self::SIGN_MODULE, self::NPC_MODULE, self::HUB_COMMAND_MODULE];
    }
}