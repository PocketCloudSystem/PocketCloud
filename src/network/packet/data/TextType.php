<?php

namespace pocketcloud\cloud\network\packet\data;

use pocketcloud\cloud\util\trait\EnumHelperTrait;

enum TextType {
    use EnumHelperTrait;

    case MESSAGE;
    case POPUP;
    case TIP;
    case TITLE;
    case ACTION_BAR;
    case TOAST_NOTIFICATION;

    public function getName(): string {
        return $this->name;
    }
}