<?php

namespace pocketcloud\cloud\console\log\output\trait;

use pocketcloud\cloud\console\log\logger\ILogger;

trait AuthorizedLoggerBase {

    private array $authorizedLoggers = [];

    public function addAuthorizedLogger(ILogger $logger): void {
        $this->authorizedLoggers[spl_object_id($logger)] = true;
    }

    public function removeAuthorizedLogger(ILogger $logger): void {
        unset($this->authorizedLoggers[spl_object_id($logger)]);
    }

    public function isAuthorizedLogger(ILogger $logger): bool {
        return isset($this->authorizedLoggers[spl_object_id($logger)]);
    }
}