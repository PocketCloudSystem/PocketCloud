<?php

namespace pocketcloud\cloud\migration;

interface IMigrator {

    public function requiresMigration(): bool;

    public function backup(): bool;

    public function migrate(): bool;

    public function rollback(): bool;

    public function id(): string;

    public function currentBackupId(): string;

    public function currentBackupPath(): string;

    public function runOnStartup(): bool;
}