<?php

namespace pocketcloud\rest;

use pocketcloud\config\CloudConfig;
use pocketcloud\lib\express\io\Request;

class RestUtils {

    public static function checkAuthorized(Request $request): bool {
        return $request->getHeaders()->has("auth-key") && $request->getHeaders()->get("auth-key") == CloudConfig::getInstance()->getRestAPIAuthKey();
    }
}