<?php

namespace pocketcloud\cloud\http\server\socket\auth;

use pocketcloud\cloud\http\server\io\Request;

final class NoAuthRequiredAuthentication implements Authentication {

    public function authenticate(Request $request): bool {
        return true;
    }
}
