<?php

namespace pocketcloud\cloud\console\command\flag;

use RuntimeException;

/**
 * -y (short)    ] just
 * --yes (long)  ] examples
 */
abstract class CommandFlag {

    /**
     * @param string $prefix
     * @param string $flag
     * @param int $characterLimit
     * @param bool $global $global means usable across the entire Command + SubCommands
     * @param bool $expectValue If $expectValue is false and the flag is set (command ... -y), the flag will have the value true (boolean)
     */
    public function __construct(
        protected readonly string $prefix,
        protected readonly string $flag,
        protected readonly int $characterLimit,
        protected readonly bool $global = false,
        protected readonly bool $expectValue = false
    ) {
        if (strlen($this->flag) > $this->characterLimit || strlen($this->flag) == 0) {
            throw new RuntimeException("$this->flag should be greater than 0 and less or equal to $this->characterLimit");
        }
    }

    abstract public static function isLikelyAFlag(string $arg): bool;

    public function buildUsage(): string {
        return "[" . $this->getFullFlag() . ($this->expectValue ? "=..." : "") . "]";
    }

    public function getFullFlag(): string {
        return $this->prefix . $this->flag;
    }

    public function getFlag(): string {
        return $this->flag;
    }

    public function getCharacterLimit(): int {
        return $this->characterLimit;
    }

    public function isGlobal(): bool {
        return $this->global;
    }

    public function isExpectValue(): bool {
        return $this->expectValue;
    }

    /**
     * @param string $flag -y [pattern: -{single letter}]
     * @param bool $global
     * @param bool $expectValue
     * @return CommandShortFlag
     */
    public static function short(string $flag, bool $global = false, bool $expectValue = false): CommandShortFlag {
        return CommandShortFlag::create($flag, $global, $expectValue);
    }

    /**
     * @param string $flag --what-ever [pattern: --{up-to-32-characters}]
     * @param bool $global
     * @param bool $expectValue
     * @return CommandLongFlag
     */
    public static function long(string $flag, bool $global = false, bool $expectValue = false): CommandLongFlag {
        return CommandLongFlag::create($flag, $global, $expectValue);
    }
}