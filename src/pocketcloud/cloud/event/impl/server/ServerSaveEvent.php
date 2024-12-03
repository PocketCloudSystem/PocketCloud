<?php

namespace pocketcloud\cloud\event\impl\server;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;

class ServerSaveEvent extends ServerEvent implements Cancelable {
    use CancelableTrait;
}