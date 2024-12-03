<?php

namespace pocketcloud\cloud\event\impl\template;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\template\Template;

abstract class TemplateEvent extends Event {

    public function __construct(private readonly Template $template) {}

    public function getTemplate(): Template {
        return $this->template;
    }
}