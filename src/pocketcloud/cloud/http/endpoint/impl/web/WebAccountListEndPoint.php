<?php

namespace pocketcloud\cloud\http\endpoint\impl\web;

use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\web\WebAccount;
use pocketcloud\cloud\web\WebAccountManager;

class WebAccountListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/webaccount/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return array_map(fn(WebAccount $account) => $account->toArray(), array_values(WebAccountManager::getInstance()->getAll()));
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}