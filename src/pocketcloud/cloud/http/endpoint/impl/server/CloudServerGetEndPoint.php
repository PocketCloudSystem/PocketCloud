<?php

namespace pocketcloud\cloud\http\endpoint\impl\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

class CloudServerGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/server/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier");

        if (($server = CloudServerManager::getInstance()->get($identifier)) !== null) {
            return $server->toDetailedArray();
        } else if (($template = TemplateManager::getInstance()->get($identifier)) !== null) {
            return array_values(array_map(fn(CloudServer $cloudServer) => $cloudServer->toDetailedArray(), CloudServerManager::getInstance()->getAll($template)));
        } else {
            return ["error" => "The server doesn't exists!"];
        }
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("identifier");
    }
}