<?php

namespace pocketcloud\http\endpoint\impl\web;

use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\web\WebAccountManager;

class WebAccountRemoveEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::DELETE, "/webaccount/remove/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");

        if (($account = WebAccountManager::getInstance()->getAccount($name)) === null) {
            return ["error" => "A web account with that name doesn't exists!"];
        }

        WebAccountManager::getInstance()->removeAccount($account);
        return ["success" => "The web account has been successfully removed!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("name");
    }
}