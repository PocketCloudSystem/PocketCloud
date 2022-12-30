<?php

namespace pocketcloud\rest\endpoint\impl\server;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;

class CloudServerStartEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/server/start/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("template");
        $count = 1;
        if ($request->data()->queries()->has("count")) if (is_numeric($request->data()->queries()->get("count"))) if (intval($request->data()->queries()->get("count")) > 0) $count = intval($request->data()->queries()->get("count"));
        $template = TemplateManager::getInstance()->getTemplateByName($name);

        if ($template === null) {
            return ["error" => "The template doesn't exists!"];
        }

        if (count(CloudServerManager::getInstance()->getServersByTemplate($template)) >= $template->getMaxServerCount()) {
            return ["error" => "The max server count is already reached!"];
        }

        CloudServerManager::getInstance()->startServer($template, $count);
        return ["success" => "Successfully trying to start " . $count . " server" . ($count == 1 ? "" : "s") . "!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("template");
    }
}