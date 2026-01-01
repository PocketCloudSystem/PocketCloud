<?php

namespace pocketcloud\cloud\provider\migration;

final class V3ToV4Migrator implements IMigrator {

    public function checkForMigration(): bool {
        return false;
    }

    public function migrate(): bool {
        return false;
    }
}