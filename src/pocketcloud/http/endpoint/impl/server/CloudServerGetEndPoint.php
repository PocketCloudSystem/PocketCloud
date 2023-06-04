<?php

namespace pocketcloud\http\endpoint\impl\server;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;

class CloudServerGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/server/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier"); //server by name, servers by template

        if (($server = CloudServerManager::getInstance()->getServerByName($identifier)) !== null) {
            return $server->toArray();
        } else if (($template = TemplateManager::getInstance()->getTemplateByName($identifier)) !== null) {
            return array_values(array_map(fn(CloudServer $cloudServer) => $cloudServer->toArray(), CloudServerManager::getInstance()->getServersByTemplate($template)));
        } else {
            return ["error" => "The server doesn't exists!"];
        }
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("identifier");
    }
}