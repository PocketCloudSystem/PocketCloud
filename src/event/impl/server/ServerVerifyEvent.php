<?php

namespace pocketcloud\cloud\event\impl\server;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;

class ServerVerifyEvent extends ServerEvent implements Cancelable {
    use CancelableTrait;
}