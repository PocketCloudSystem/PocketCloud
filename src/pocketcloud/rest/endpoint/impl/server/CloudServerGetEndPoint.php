<?php

namespace pocketcloud\rest\endpoint\impl\server;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
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