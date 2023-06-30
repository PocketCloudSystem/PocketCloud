<?php

namespace pocketcloud\event\impl\template;

use pocketcloud\template\Template;

class TemplateEditEvent extends TemplateEvent {

    public function __construct(
        Template $template,
        private readonly ?bool $lobby,
        private readonly ?bool $maintenance,
        private readonly ?bool $static,
        private readonly ?int $maxPlayerCount,
        private readonly ?int $minServerCount,
        private readonly ?int $maxServerCount,
        private readonly ?bool $startNewWhenFull,
        private readonly ?bool $autoStart
    ) {
        parent::__construct($template);
    }

    public function getLobby(): ?bool {
        return $this->lobby;
    }

    public function getMaintenance(): ?bool {
        return $this->maintenance;
    }

    public function getStatic(): ?bool {
        return $this->static;
    }

    public function getMaxPlayerCount(): ?int {
        return $this->maxPlayerCount;
    }

    public function getMinServerCount(): ?int {
        return $this->minServerCount;
    }

    public function getMaxServerCount(): ?int {
        return $this->maxServerCount;
    }

    public function getStartNewWhenFull(): ?bool {
        return $this->startNewWhenFull;
    }

    public function getAutoStart(): ?bool {
        return $this->autoStart;
    }
}