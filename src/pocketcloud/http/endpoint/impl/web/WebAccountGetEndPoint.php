<?php

namespace pocketcloud\http\endpoint\impl\web;

use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\web\WebAccountManager;

class WebAccountGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/webaccount/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");

        if (($account = WebAccountManager::getInstance()->getAccount($name)) === null) {
            return ["error" => "A web account with that name doesn't exists!"];
        }

        return $account->toArray();
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("name");
    }
}