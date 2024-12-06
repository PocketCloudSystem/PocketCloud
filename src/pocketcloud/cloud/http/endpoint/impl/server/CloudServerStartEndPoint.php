<?php

namespace pocketcloud\cloud\http\endpoint\impl\server;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\TemplateManager;

class CloudServerStartEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/server/start/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("template");
        $count = 1;
        if ($request->data()->queries()->has("count")) if (is_numeric($request->data()->queries()->get("count"))) if (intval($request->data()->queries()->get("count")) > 0) $count = intval($request->data()->queries()->get("count"));
        $template = TemplateManager::getInstance()->get($name);

        if ($template === null) {
            return ["error" => "The template doesn't exists!"];
        }

        if (!CloudServerManager::getInstance()->canStartMore($template)) {
            return ["error" => "The max server count is already reached!"];
        }

        CloudServerManager::getInstance()->start($template, $count);
        return ["success" => "Successfully trying to start " . $count . " server" . ($count == 1 ? "" : "s") . "!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("template");
    }
}