<?php

namespace pocketcloud\update;

use pocketcloud\scheduler\AsyncPool;
use pocketcloud\utils\SingletonTrait;
use pocketcloud\utils\VersionInfo;

class UpdateChecker {
    use SingletonTrait;

    private array $data = [];

    public function __construct() {
        self::setInstance($this);
        $this->check();
    }

    public function check() {
        AsyncPool::getInstance()->submitTask(new AsyncUpdateCheckTask());
    }

    public function isOutdated(): ?bool {
        return $this->data["outdated"] ?? null;
    }

    public function isUpToDate(): bool {
        return !$this->isOutdated();
    }

    public function getNewestVersion(): ?string {
        return $this->data["newest_version"] ?? null;
    }

    public function getCurrentVersion(): string {
        return VersionInfo::VERSION;
    }

    public function setData(array $data): void {
        $this->data = $data;
    }

    public function getData(): array {
        return $this->data;
    }
}