<?php

namespace pocketcloud\config\impl;

use configlib\Configuration;
use pocketcloud\util\ExceptionHandler;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;

class ModuleConfig extends Configuration implements Reloadable {
    use SingletonTrait;

    private bool $signModule = true;
    private bool $npcModule = true;
    private bool $globalChatModule = false;
    private bool $hubCommandModule = true;

    public function __construct() {
        self::setInstance($this);
        parent::__construct(IN_GAME_PATH . "modules.json", self::TYPE_JSON);
        if (!$this->load()) $this->save();
    }

    public function load(): bool {
        return ExceptionHandler::tryCatch(fn() => parent::load()) ?? false;
    }

    public function save(): bool {
        return ExceptionHandler::tryCatch(fn() => parent::save()) ?? false;
    }

    public function reload(): bool {
        $this->signModule = true;
        $this->npcModule = true;
        $this->globalChatModule = false;
        $this->hubCommandModule = true;
        return $this->load();
    }

    public function isSignModule(): bool {
        return $this->signModule;
    }

    public function isNpcModule(): bool {
        return $this->npcModule;
    }

    public function isGlobalChatModule(): bool {
        return $this->globalChatModule;
    }

    public function isHubCommandModule(): bool {
        return $this->hubCommandModule;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}