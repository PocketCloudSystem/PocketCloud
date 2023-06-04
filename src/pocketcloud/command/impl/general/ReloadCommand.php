<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\PocketCloud;

class ReloadCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (!PocketCloud::getInstance()->isReloading()) {
            PocketCloud::getInstance()->reload();
        }
        return true;
    }
}