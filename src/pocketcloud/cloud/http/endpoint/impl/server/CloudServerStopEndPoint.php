<?php

namespace pocketcloud\cloud\http\endpoint\impl\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

final class CloudServerStopEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/server/stop/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $identifier = $request->data()->queries()->get("identifier");

        if (($server = CloudServerManager::getInstance()->get($identifier)) !== null) {
            CloudServerManager::getInstance()->stop($server);
            return ["success" => "The server was successfully stopped!"];
        } else if (($template = TemplateManager::getInstance()->get($identifier)) !== null) {
            CloudServerManager::getInstance()->stop($template);
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