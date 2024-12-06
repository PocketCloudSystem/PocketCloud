<?php

namespace pocketcloud\cloud\http\endpoint\impl\web;

use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\web\WebAccountManager;

class WebAccountGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/webaccount/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");

        if (($account = WebAccountManager::getInstance()->get($name)) === null) {
            return ["error" => "A web account with that name doesn't exists!"];
        }

        return $account->toArray();
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("name");
    }
}