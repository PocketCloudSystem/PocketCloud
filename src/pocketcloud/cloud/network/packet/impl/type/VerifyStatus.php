<?php

namespace pocketcloud\cloud\network\packet\impl\type;

use pocketcloud\cloud\util\enum\EnumTrait;

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

    public function __construct(private readonly string $name) {}

    public function getName(): string {
        return $this->name;
    }
}