<?php

namespace pocketcloud\config;

use pocketcloud\lib\config\Configuration;
use pocketcloud\utils\SingletonTrait;

class ModulesConfig extends Configuration {
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

    public function reload(): void {
        $this->signModule = true;
        $this->npcModule = true;
        $this->globalChatModule = false;
        $this->hubCommandModule = true;
        $this->load();
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
}