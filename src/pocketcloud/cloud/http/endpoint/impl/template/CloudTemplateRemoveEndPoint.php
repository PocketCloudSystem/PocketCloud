<?php

namespace pocketcloud\cloud\http\endpoint\impl\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\template\TemplateManager;

class CloudTemplateRemoveEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::DELETE, "/template/remove/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $template = TemplateManager::getInstance()->get($name);

        if ($template === null) {
            return ["error" => "The template doesn't exists!"];
        }

        TemplateManager::getInstance()->remove($template);
        return ["success" => "The template was removed!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("name");
    }
}