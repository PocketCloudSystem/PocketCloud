<?php

namespace pocketcloud\event\impl\server;

use pocketcloud\event\Cancelable;
use pocketcloud\event\CancelableTrait;

class ServerSaveEvent extends ServerEvent implements Cancelable {
    use CancelableTrait;
}