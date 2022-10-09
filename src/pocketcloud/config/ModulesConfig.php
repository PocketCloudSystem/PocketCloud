<?php

namespace pocketcloud\config;

use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;

class ModulesConfig {
    use SingletonTrait;

    private Config $config;
    private bool $signModule;
    private bool $npcModule;
    private bool $globalChatModule;
    private bool $hubCommandModule;

    public function __construct() {
        self::setInstance($this);
        $this->config = new Config(IN_GAME_PATH . "modules.json", 1);

        if (!$this->config->exists("sign-module")) $this->config->set("sign-module", true);
        if (!$this->config->exists("npc-module")) $this->config->set("npc-module", true);
        if (!$this->config->exists("global-chat-module")) $this->config->set("global-chat-module", false);
        if (!$this->config->exists("hubcommand-module")) $this->config->set("hubcommand-module", true);
        $this->config->save();

        $this->load();
    }

    private function load(): void {
        $this->signModule = $this->getConfig()->get("sign-module");
        $this->npcModule = $this->getConfig()->get("npc-module");
        $this->globalChatModule = $this->getConfig()->get("global-chat-module");
        $this->hubCommandModule = $this->getConfig()->get("hubcommand-module");
    }

    public function reload(): void {
        $this->config->reload();
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

    public function getConfig(): Config {
        return $this->config;
    }
}