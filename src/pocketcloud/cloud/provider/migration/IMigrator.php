<?php

namespace pocketcloud\cloud\provider\migration;

interface IMigrator {

    public function checkForMigration(): bool;

    public function migrate(): bool;
}