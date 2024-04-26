<?php

namespace pocketcloud\http\endpoint\impl\web;

use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\web\WebAccountManager;
use pocketcloud\web\WebAccountRoles;

class WebAccountUpdateEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::PATCH, "/webaccount/update/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $action = strtolower($request->data()->queries()->get("action"));
        $value = $request->data()->queries()->get("value");

        if (($account = WebAccountManager::getInstance()->getAccount($name)) === null) {
            return ["error" => "A web account with that name doesn't exists!"];
        }

        if (!in_array($action, ["role", "password"])) {
            return ["error" => "Please provide a valid action! (role, password)"];
        }

        if ($action == "password") {
            WebAccountManager::getInstance()->updateAccount($account, $value, null);
        } else {
            if (($role = WebAccountRoles::from($value)) !== null) {
                WebAccountManager::getInstance()->updateAccount($account, null, $role);
            } else return ["error" => "Please provide a valid role! (admin, default)"];
        }

        return ["success" => "The web account has been updated!"];
    }

    public function isBadRequest(Request $request): bool {
        if ($request->data()->queries()->has("name") && $request->data()->queries()->has("action") && $request->data()->queries()->has("value")) return false;
        return true;
    }
}