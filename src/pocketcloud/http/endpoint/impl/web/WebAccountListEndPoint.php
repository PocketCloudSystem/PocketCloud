<?php

namespace pocketcloud\http\endpoint\impl\web;

use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\web\WebAccount;
use pocketcloud\web\WebAccountManager;

class WebAccountListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/webaccount/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        return array_map(fn(WebAccount $account) => $account->getName(), array_values(WebAccountManager::getInstance()->getAccounts()));
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}