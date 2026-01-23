<?php

namespace pocketcloud\cloud\http\socket\auth;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\socket\SocketClient;

final class DefaultAuthentication implements Authentication {

    public function authenticate(SocketClient $client, Request $request): bool {
        return $request->getHeader("auth-key") === MainConfig::getInstance()->getHttpServerAuthKey();
    }
}