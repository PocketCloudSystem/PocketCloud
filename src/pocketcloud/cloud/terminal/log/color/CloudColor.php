<?php

namespace pocketcloud\cloud\terminal\log\color;

use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static CloudColor BLACK()
 * @method static CloudColor WHITE()
 * @method static CloudColor DARK_GRAY()
 * @method static CloudColor GRAY()
 * @method static CloudColor BLUE()
 * @method static CloudColor DARK_BLUE()
 * @method static CloudColor DARK_CYAN()
 * @method static CloudColor CYAN()
 * @method static CloudColor DARK_RED()
 * @method static CloudColor RED()
 * @method static CloudColor DARK_GREEN()
 * @method static CloudColor GREEN()
 * @method static CloudColor MAGENTA()
 * @method static CloudColor PINK()
 * @method static CloudColor YELLOW()
 * @method static CloudColor ORANGE()
 * @method static CloudColor RESET()
 */
final class CloudColor {
    use EnumTrait;

    private static function registerColor(CloudColor $color): void {
        self::register($color->getName(), $color);
   }

    protected static function init(): void {
        self::registerColor(new CloudColor("black", "§0", "\x1b[38;5;16m"));
        self::registerColor(new CloudColor("white", "§f", "\x1b[38;5;231m"));
        self::registerColor(new CloudColor("dark_gray", "§8", "\x1b[38;5;59m"));
        self::registerColor(new CloudColor("gray", "§7", "\x1b[38;5;145m"));
        self::registerColor(new CloudColor("blue", "§9", "\x1b[38;5;63m"));
        self::registerColor(new CloudColor("dark_blue", "§1", "\x1b[38;5;19m"));
        self::registerColor(new CloudColor("dark_cyan", "§3", "\x1b[38;5;37m"));
        self::registerColor(new CloudColor("cyan", "§b", "\x1b[38;5;87m"));
        self::registerColor(new CloudColor("dark_red", "§4", "\x1b[38;5;124m"));
        self::registerColor(new CloudColor("red", "§c", "\x1b[38;5;203m"));
        self::registerColor(new CloudColor("dark_green", "§2", "\x1b[38;5;34m"));
        self::registerColor(new CloudColor("green", "§a", "\x1b[38;5;83m"));
        self::registerColor(new CloudColor("magenta", "§5", "\x1b[38;5;127m"));
        self::registerColor(new CloudColor("pink", "§d", "\x1b[38;5;207m"));
        self::registerColor(new CloudColor("yellow", "§e", "\x1b[38;5;227m"));
        self::registerColor(new CloudColor("orange", "§6", "\x1b[38;5;214m"));
        self::registerColor(new CloudColor("reset", "§r", "\x1b[m"));
    }

    public static function toColoredString(string $message, bool $formatting = true): string {
        foreach (self::getColors() as $color) {
            $message = str_replace($color->getColorCode(), ($formatting ? $color->getColor() : ""), $message);
        }
        return $message;
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

    /** @return array<CloudColor> */
    public static function getColors(): array {
        self::check();
        return self::$members;
    }
}