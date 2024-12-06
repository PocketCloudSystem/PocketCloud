<?php

namespace pocketcloud\cloud\http\endpoint\impl\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\server\CloudServerManager;

class CloudServerSaveEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/server/save/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("server");
        $server = CloudServerManager::getInstance()->get($name);

        if ($server === null) {
            return ["error" => "The server doesn't exists!"];
        }

        CloudServerManager::getInstance()->save($server);
        return ["success" => "The cloud is successfully trying to save the given server!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("server");
    }
}