<?php

namespace pocketcloud\cloud\cache;

final class InGameModule {

    public const string SIGN_MODULE = "sign_module";
    public const string NPC_MODULE = "npc_module";
    public const string HUB_COMMAND_MODULE = "hub_command_module";

    private static array $moduleStates = [
        self::SIGN_MODULE => false,
        self::NPC_MODULE => false,
        self::HUB_COMMAND_MODULE => false
    ];

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