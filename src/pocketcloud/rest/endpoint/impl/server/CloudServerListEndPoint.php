<?php

namespace pocketcloud\rest\endpoint\impl\server;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;

class CloudServerListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/server/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $template = $request->data()->queries()->get("template");

        if ($template === null) {
            return array_values(array_map(fn(CloudServer $cloudServer) => $cloudServer->getName(), CloudServerManager::getInstance()->getServers()));
        } else {
            if (($template = TemplateManager::getInstance()->getTemplateByName($template)) !== null) {
                return array_values(array_map(fn(CloudServer $cloudServer) => $cloudServer->getName(), CloudServerManager::getInstance()->getServersByTemplate($template)));
            } else {
                return ["error" => "The template doesn't exists!"];
            }
        }
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}