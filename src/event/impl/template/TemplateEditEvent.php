<?php

namespace pocketcloud\cloud\event\impl\template;

use pocketcloud\cloud\template\Template;

class TemplateEditEvent extends TemplateEvent {

    public function __construct(
        Template $template,
        protected readonly ?bool $lobby,
        protected readonly ?bool $maintenance,
        protected readonly ?bool $static,
        protected readonly ?int $maxPlayerCount,
        protected readonly ?int $minServerCount,
        protected readonly ?int $maxServerCount,
        protected readonly ?float $startNewPercentage,
        protected readonly ?bool $autoStart
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

    public function getStartNewPercentage(): ?float {
        return $this->startNewPercentage;
    }

    public function getAutoStart(): ?bool {
        return $this->autoStart;
    }
}