<?php

namespace pocketcloud\event\impl\template;

use pocketcloud\event\Event;
use pocketcloud\template\Template;

abstract class TemplateEvent extends Event {

    public function __construct(private Template $template) {}

    public function getTemplate(): Template {
        return $this->template;
    }
}