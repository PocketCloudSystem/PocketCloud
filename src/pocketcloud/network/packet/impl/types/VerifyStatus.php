<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\util\EnumTrait;

/**
 * @method static VerifyStatus DENIED()
 * @method static VerifyStatus VERIFIED()
 * @method static VerifyStatus NOT_APPLIED()
 */
final class VerifyStatus {
    use EnumTrait;

    protected static function init(): void {
        self::register("DENIED", new VerifyStatus("DENIED"));
        self::register("VERIFIED", new VerifyStatus("VERIFIED"));
        self::register("NOT_APPLIED", new VerifyStatus("NOT_APPLIED"));
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