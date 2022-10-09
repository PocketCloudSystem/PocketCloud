<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\utils\EnumTrait;

/**
 * @method static TextType MESSAGE()
 * @method static TextType POPUP()
 * @method static TextType TIP()
 * @method static TextType TITLE()
 * @method static TextType ACTION_BAR()
 */
final class TextType {
    use EnumTrait;

    protected static function init(): void {
        self::register("message", new TextType("MESSAGE"));
        self::register("popup", new TextType("POPUP"));
        self::register("tip", new TextType("TIP"));
        self::register("title", new TextType("TITLE"));
        self::register("action_bar", new TextType("ACTION_BAR"));
    }

    public static function getTypeByName(string $name): ?TextType {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    public function __construct(private string $name) {}

    public function getName(): string {
        return $this->name;
    }
}