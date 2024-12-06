<?php

namespace pocketcloud\cloud\http\endpoint\impl\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

class CloudServerListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/server/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $template = $request->data()->queries()->get("template");

        if ($template === null) {
            return array_values(array_map(fn(CloudServer $cloudServer) => $cloudServer->getName(), CloudServerManager::getInstance()->getAll()));
        } else {
            if (($template = TemplateManager::getInstance()->get($template)) !== null) {
                return array_values(array_map(fn(CloudServer $cloudServer) => $cloudServer->getName(), CloudServerManager::getInstance()->getAllByTemplate($template)));
            } else {
                return ["error" => "The template doesn't exists!"];
            }
        }
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}