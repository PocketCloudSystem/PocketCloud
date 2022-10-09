<?php

namespace pocketcloud\event\impl\template;

use pocketcloud\template\Template;

class TemplateEditEvent extends TemplateEvent {

    public function __construct(private Template $template, private ?bool $lobby, private ?bool $maintenance, private ?int $maxPlayerCount, private ?int $minServerCount, private ?int $maxServerCount, private ?bool $autoStart) {
        parent::__construct($this->template);
    }

    public function getLobby(): ?bool {
        return $this->lobby;
    }

    public function getMaintenance(): ?bool {
        return $this->maintenance;
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

    public function getAutoStart(): ?bool {
        return $this->autoStart;
    }
}