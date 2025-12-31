<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum ServerStatus: string {
    use EnumHelperTrait;

    case STARTING = "§2STARTING";
    case ONLINE = "§aONLINE";
    case FULL = "§eFULL";
    case IN_GAME = "§6INGAME";
    case STOPPING = "§4STOPPING";
    case OFFLINE = "§cOFFLINE";

    public function getName(): string {
        return $this->name;
    }

    public function getDisplay(): string {
        return $this->value;
    }
}