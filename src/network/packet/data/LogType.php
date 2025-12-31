<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum LogType {
    use EnumHelperTrait;

    case INFO;
    case WARN;
    case ERROR;
    case SUCCESS;
    case DEBUG;

    public function getName(): string {
        return $this->name;
    }
}