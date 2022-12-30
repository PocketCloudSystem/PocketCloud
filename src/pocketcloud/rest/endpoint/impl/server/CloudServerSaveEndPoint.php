<?php

namespace pocketcloud\rest\endpoint\impl\server;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\server\CloudServerManager;

class CloudServerSaveEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/server/save/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("server");
        $server = CloudServerManager::getInstance()->getServerByName($name);

        if ($server === null) {
            return ["error" => "The server doesn't exists!"];
        }

        CloudServerManager::getInstance()->saveServer($server);
        return ["success" => "The cloud is successfully trying to save the given server!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("server");
    }
}