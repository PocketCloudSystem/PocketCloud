<?php

namespace pocketcloud\rest\endpoint\impl\server;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;

class CloudServerStopEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/server/stop/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier");

        if (($server = CloudServerManager::getInstance()->getServerByName($identifier)) !== null) {
            CloudServerManager::getInstance()->stopServer($server);
            return ["success" => "The server was successfully stopped!"];
        } else if (($template = TemplateManager::getInstance()->getTemplateByName($identifier)) !== null) {
            CloudServerManager::getInstance()->stopTemplate($template);
            return ["success" => "The template was successfully stopped!"];
        } else if (strtolower($identifier) == "all") {
            CloudServerManager::getInstance()->stopAll();
            return ["success" => "All servers have been successfully stopped!"];
        }

        return ["error" => "The server doesn't exists!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("identifier");
    }
}