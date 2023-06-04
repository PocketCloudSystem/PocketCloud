<?php

namespace pocketcloud\http\endpoint\impl\server;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\http\endpoint\EndPoint;
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