<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\util\EnumTrait;

/**
 * @method static ErrorReason NO_ERROR()
 * @method static ErrorReason TEMPLATE_EXISTENCE()
 * @method static ErrorReason MAX_SERVERS()
 * @method static ErrorReason SERVER_EXISTENCE()
 */
final class ErrorReason {
    use EnumTrait;

    protected static function init(): void {
        self::register("no_error", new ErrorReason("NO_ERROR"));
        self::register("template_existence", new ErrorReason("TEMPLATE_EXISTENCE"));
        self::register("max_servers", new ErrorReason("MAX_SERVERS"));
        self::register("server_existence", new ErrorReason("SERVER_EXISTENCE"));
    }

    public static function getReasonByName(string $name): ?ErrorReason {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    public function __construct(private readonly string $name) {}

    public function getName(): string {
        return $this->name;
    }
}