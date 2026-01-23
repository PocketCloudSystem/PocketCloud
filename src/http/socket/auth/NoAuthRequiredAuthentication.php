<?php

namespace pocketcloud\cloud\http\socket\auth;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\socket\SocketClient;

final class NoAuthRequiredAuthentication implements Authentication {

    public function authenticate(SocketClient $client, Request $request): bool {
        return true;
    }
}