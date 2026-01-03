<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\util\misc\Writeable;
use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum ServerDisconnectReason implements Writeable {
    use EnumHelperTrait;

    case CLOUD_SHUTDOWN;
    case SERVER_SHUTDOWN;

    public function getName(): string {
        return $this->name;
    }

    public function write(): string {
        return $this->name;
    }
}