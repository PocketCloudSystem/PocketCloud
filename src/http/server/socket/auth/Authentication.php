<?php

namespace pocketcloud\cloud\http\server\socket\auth;

use pocketcloud\cloud\http\server\io\Request;

interface Authentication {

    /**
     * @param Request $request
     * @return bool return true if authenticated and false if the authentication process failed
     */
    public function authenticate(Request $request): bool;
}