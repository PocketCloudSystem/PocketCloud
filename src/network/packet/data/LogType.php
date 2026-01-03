<?php

namespace pocketcloud\cloud\network\packet\data;

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

    public function write(): string {
        return $this->name;
    }
}