<?php

namespace pocketcloud\cloud\network\packet\impl\type;

use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static TextType MESSAGE()
 * @method static TextType POPUP()
 * @method static TextType TIP()
 * @method static TextType TITLE()
 * @method static TextType ACTION_BAR()
 * @method static TextType TOAST_NOTIFICATION()
 */
final class TextType {
    use EnumTrait;

    protected static function init(): void {
        self::register("message", new TextType("MESSAGE"));
        self::register("popup", new TextType("POPUP"));
        self::register("tip", new TextType("TIP"));
        self::register("title", new TextType("TITLE"));
        self::register("action_bar", new TextType("ACTION_BAR"));
        self::register("toast_notification", new TextType("TOAST_NOTIFICATION"));
    }

    public function __construct(private readonly string $name) {}

    public function getName(): string {
        return $this->name;
    }
}