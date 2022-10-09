<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\utils\EnumTrait;

/**
 * @method static VerifyStatus NOT_VERIFIED()
 * @method static VerifyStatus VERIFIED()
 */
final class VerifyStatus {
    use EnumTrait;

    protected static function init(): void {
        self::register("NOT_VERIFIED", new VerifyStatus("NOT_VERIFIED"));
        self::register("VERIFIED", new VerifyStatus("VERIFIED"));
    }

    public static function getStatusByName(string $name): ?VerifyStatus {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    public function __construct(private string $name) {}

    public function getName(): string {
        return $this->name;
    }
}