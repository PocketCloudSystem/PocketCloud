<?php

namespace pocketcloud\http\endpoint\impl\server;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\http\endpoint\EndPoint;
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