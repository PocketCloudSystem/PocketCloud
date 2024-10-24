<?php

namespace pocketcloud\config\impl;

use configlib\Configuration;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;

final class ModuleConfig extends Configuration implements Reloadable {
    use SingletonTrait;

    private bool $signModule = true;
    private bool $npcModule = true;
    private bool $hubCommandModule = true;

    public function __construct() {
        self::setInstance($this);
        parent::__construct(IN_GAME_PATH . "modules.json", self::TYPE_JSON);
        if (!$this->load()) $this->save();
    }

    public function reload(): bool {
        $this->signModule = true;
        $this->npcModule = true;
        $this->hubCommandModule = true;
        return $this->load();
    }

    public function setSignModule(bool $signModule): void {
        $this->signModule = $signModule;
    }

    public function setNpcModule(bool $npcModule): void {
        $this->npcModule = $npcModule;
    }

    public function setHubCommandModule(bool $hubCommandModule): void {
        $this->hubCommandModule = $hubCommandModule;
    }

    public function isSignModule(): bool {
        return $this->signModule;
    }

    public function isNpcModule(): bool {
        return $this->npcModule;
    }

    public function isHubCommandModule(): bool {
        return $this->hubCommandModule;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}