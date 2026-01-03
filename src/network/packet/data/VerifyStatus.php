<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum VerifyStatus implements Writeable {
    use EnumHelperTrait;

    case DENIED;
    case VERIFIED;
    case NOT_APPLIED;

    public function getName(): string {
        return $this->name;
    }

    public function write(): string {
        return $this->name;
    }
}