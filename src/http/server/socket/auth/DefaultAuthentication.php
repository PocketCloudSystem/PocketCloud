<?php

namespace pocketcloud\cloud\http\server\socket\auth;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\http\server\io\Request;

final class DefaultAuthentication implements Authentication {

    public function authenticate(Request $request): bool {
        return $request->getHeader("auth-key") === MainConfig::getInstance()->getHttpServerAuthKey();
    }
}