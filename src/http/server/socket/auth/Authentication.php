<?php

namespace pocketcloud\cloud\http\server\socket\auth;

use pocketcloud\cloud\http\server\io\Request;
use pocketcloud\cloud\http\server\socket\SocketClient;

interface Authentication {

    /**
     * @param SocketClient $client
     * @param Request $request
     * @return bool return true if authenticated and false if the authentication process failed
     */
    public function authenticate(SocketClient $client, Request $request): bool;
}