<?php

namespace pocketcloud\event\impl\software;

use pocketcloud\event\Event;
use pocketcloud\software\Software;

abstract class SoftwareEvent extends Event {

    public function __construct(private Software $software) {}

    public function getSoftware(): Software {
        return $this->software;
    }
}