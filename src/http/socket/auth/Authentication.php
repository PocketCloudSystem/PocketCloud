<?php

namespace pocketcloud\cloud\http\socket\auth;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\socket\SocketClient;

interface Authentication {

    /**
     * @param SocketClient $client
     * @param Request $request
     * @return bool return true if authenticated and false if the authentication process failed
     */
    public function authenticate(SocketClient $client, Request $request): bool;
}