<?php

namespace pocketcloud\cloud\console\log\color;

use pocketcloud\cloud\util\trait\RegistryTrait;

/**
 * @method static CloudConsoleColor BLACK()
 * @method static CloudConsoleColor WHITE()
 * @method static CloudConsoleColor DARK_GRAY()
 * @method static CloudConsoleColor GRAY()
 * @method static CloudConsoleColor BLUE()
 * @method static CloudConsoleColor DARK_BLUE()
 * @method static CloudConsoleColor DARK_CYAN()
 * @method static CloudConsoleColor CYAN()
 * @method static CloudConsoleColor DARK_RED()
 * @method static CloudConsoleColor RED()
 * @method static CloudConsoleColor DARK_GREEN()
 * @method static CloudConsoleColor LIME()
 * @method static CloudConsoleColor MAGENTA()
 * @method static CloudConsoleColor PINK()
 * @method static CloudConsoleColor YELLOW()
 * @method static CloudConsoleColor ORANGE()
 * @method static CloudConsoleColor RESET()
 */
final class CloudConsoleColor {
    use RegistryTrait {
        register as _register;
    }

    private static function register(CloudConsoleColor $color): void {
        self::_register($color->getName(), $color);
   }

    protected static function init(): void {
        self::register(new CloudConsoleColor("black", "§0", "\x1b[38;5;16m"));
        self::register(new CloudConsoleColor("white", "§f", "\x1b[38;5;231m"));
        self::register(new CloudConsoleColor("dark_gray", "§8", "\x1b[38;5;59m"));
        self::register(new CloudConsoleColor("gray", "§7", "\x1b[38;5;145m"));
        self::register(new CloudConsoleColor("blue", "§9", "\x1b[38;5;63m"));
        self::register(new CloudConsoleColor("dark_blue", "§1", "\x1b[38;5;19m"));
        self::register(new CloudConsoleColor("dark_cyan", "§3", "\x1b[38;5;37m"));
        self::register(new CloudConsoleColor("cyan", "§b", "\x1b[38;5;87m"));
        self::register(new CloudConsoleColor("dark_red", "§4", "\x1b[38;5;124m"));
        self::register(new CloudConsoleColor("red", "§c", "\x1b[38;5;203m"));
        self::register(new CloudConsoleColor("dark_green", "§2", "\x1b[38;5;34m"));
        self::register(new CloudConsoleColor("lime", "§a", "\x1b[38;5;83m"));
        self::register(new CloudConsoleColor("magenta", "§5", "\x1b[38;5;127m"));
        self::register(new CloudConsoleColor("pink", "§d", "\x1b[38;5;207m"));
        self::register(new CloudConsoleColor("yellow", "§e", "\x1b[38;5;227m"));
        self::register(new CloudConsoleColor("orange", "§6", "\x1b[38;5;214m"));
        self::register(new CloudConsoleColor("reset", "§r", "\x1b[m"));
    }

    public static function toColoredString(string $message, bool $formatting = true): string {
        foreach (self::getColors() as $color) $message = str_replace($color->getColorCode(), ($formatting ? $color->getColor() : ""), $message);
        return $message;
    }

    public static function stripColors(string $text): string {
        return preg_replace("/§[0-9a-fk-or]/", "", $text);
    }

    public function __construct(
        private readonly string $name,
        private readonly string $colorCode,
        private readonly string $color
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function getColorCode(): string {
        return $this->colorCode;
    }

    public function getColor(): string {
        return $this->color;
    }

    public function __toString(): string {
        return $this->colorCode;
    }

    /** @return array<CloudConsoleColor> */
    public static function getColors(): array {
        self::check();
        return self::$members;
    }
}