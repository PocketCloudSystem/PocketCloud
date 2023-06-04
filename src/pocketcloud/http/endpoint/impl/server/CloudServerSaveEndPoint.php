<?php

namespace pocketcloud\http\endpoint\impl\server;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\http\endpoint\EndPoint;
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