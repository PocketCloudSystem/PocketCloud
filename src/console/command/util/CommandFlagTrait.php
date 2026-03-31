<?php

namespace pocketcloud\cloud\console\command\util;

use pocketcloud\cloud\console\command\flag\CommandFlag;
use pocketcloud\cloud\console\command\flag\CommandLongFlag;
use pocketcloud\cloud\console\command\flag\CommandShortFlag;
use pocketcloud\cloud\console\command\parameter\exception\FlagParseException;

trait CommandFlagTrait {

    /** @var array<CommandFlag> */
    private array $flags = [];

    /**
     * This function will only check for short and long command flags, nothing else
     * @param array $args
     * @param bool $mergeGlobalAndRegularFlags
     * @return array{globalFlags: array, regularFlags: array}
     * @throws FlagParseException
     * @see CommandLongFlag
     * @see CommandShortFlag
     */
    public function scanAndCleanFlags(array &$args, bool $mergeGlobalAndRegularFlags = false): array {
        if (empty($this->flags)) return ["globalFlags" => [], "regularFlags" => []];
        $globalFlags = [];
        $regularFlags = [];
        foreach ($args as $i => $arg) {
            $parts = [];
            $remove = false;

            if (CommandLongFlag::isLikelyAFlag($arg)) {
                $flagPart = substr(str_contains($arg, CommandLongFlag::PREFIX) ? ($parts = explode("=", $arg, 2))[0] : $arg, strlen(CommandLongFlag::PREFIX));
                $fullFlagPart = CommandLongFlag::PREFIX . $flagPart;
                $valuePart = $parts[1] ?? null;

                if (isset($this->flags[$fullFlagPart])) {
                    $remove = true;
                    $flag = $this->flags[$fullFlagPart];
                    if ($flag->isExpectValue()) {
                        if ($valuePart === null) throw new FlagParseException();
                        if ($flag->isGlobal() && !$mergeGlobalAndRegularFlags) $globalFlags[$flagPart] = $valuePart;
                        else $regularFlags[$flagPart] = $valuePart;
                    } else {
                        if ($flag->isGlobal() && !$mergeGlobalAndRegularFlags) $globalFlags[$flagPart] = true;
                        else $regularFlags[$flagPart] = true;
                    }
                }
            } else if (CommandShortFlag::isLikelyAFlag($arg)) {
                // Short flags have one unique feature: they can be inside one (-), for example: -abc (flag a, b and c are set)
                $flagPart = substr(str_contains($arg, "=") ? ($parts = explode("=", $arg, 2))[0] : $arg, strlen(CommandShortFlag::PREFIX));
                $fullFlagPart = CommandShortFlag::PREFIX . $flagPart;
                $valuePart = $parts[1] ?? null;

                if (isset($this->flags[$fullFlagPart])) {
                    $remove = true;
                    $flag = $this->flags[$fullFlagPart];
                    if ($flag->isExpectValue()) {
                        if ($valuePart === null) throw new FlagParseException();
                        if ($flag->isGlobal() && !$mergeGlobalAndRegularFlags) $globalFlags[$flagPart] = $valuePart;
                        else $regularFlags[$flagPart] = $valuePart;
                    } else {
                        if ($flag->isGlobal() && !$mergeGlobalAndRegularFlags) $globalFlags[$flagPart] = true;
                        else $regularFlags[$flagPart] = true;
                    }
                } else {
                    // When you have the format -abc (three flags into one -), no value will be accepted
                    $everyFlagPart = str_split($flagPart);
                    foreach ($everyFlagPart as $char) {
                        $fullFlagChar = CommandShortFlag::PREFIX . $char;
                        if (isset($this->flags[$fullFlagChar])) {
                            $remove = true;
                            $charFlag = $this->flags[$fullFlagChar];
                            if ($charFlag->isGlobal() && !$mergeGlobalAndRegularFlags) $globalFlags[$char] = true;
                            else $regularFlags[$char] = true;
                        }
                    }
                }
            }

            if ($remove) unset($args[$i]);
        }

        $args = array_values($args);

        return ["globalFlags" => $globalFlags, "regularFlags" => $regularFlags];
    }

    public function addFlag(CommandFlag $flag): self {
        $this->flags[$flag->getFullFlag()] = $flag;
        return $this;
    }

    public function addFlags(CommandFlag... $flags): self {
        foreach ($flags as $flag) $this->addFlag($flag);
        return $this;
    }

    public function getFlag(string $flag): ?CommandFlag {
        return $this->flags[$flag] ?? null;
    }

    public function getFlags(): array {
        return $this->flags;
    }
}