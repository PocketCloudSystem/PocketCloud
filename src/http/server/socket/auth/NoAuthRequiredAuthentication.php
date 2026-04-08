<?php

namespace pocketcloud\cloud\http\server\socket\auth;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\socket\SocketClient;

final class NoAuthRequiredAuthentication implements Authentication {

    public function authenticate(SocketClient $client, Request $request): bool {
        return true;
    }
}