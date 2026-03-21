<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum LogType implements Writeable {
    use EnumHelperTrait;

    case INFO;
    case WARN;
    case ERROR;
    case SUCCESS;
    case DEBUG;

    public function getName(): string {
        return $this->name;
    }

    public function toLogLevel(): CloudLogLevel {
        return match ($this) {
            self::INFO => CloudLogLevel::INFO(),
            self::WARN => CloudLogLevel::WARN(),
            self::ERROR => CloudLogLevel::ERROR(),
            self::SUCCESS => CloudLogLevel::SUCCESS(),
            self::DEBUG => CloudLogLevel::DEBUG()
        };
    }

    public function write(): string {
        return $this->name;
    }
}