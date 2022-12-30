<?php

namespace pocketcloud\rest\endpoint\impl\template;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\template\TemplateManager;

class CloudTemplateEditEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::PATCH, "/template/edit/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $editKey = $request->data()->queries()->get("key");
        $editValue = $request->data()->queries()->get("value");
        $template = TemplateManager::getInstance()->getTemplateByName($name);

        if ($template === null) {
            return ["error" => "The template doesn't exists!"];
        }
        
        if (!$template::isValidEditKey($editKey)) {
            return ["error" => "The given key doesn't exists!"];
        }
        
        if (!$template::isValidEditValue($editValue, $editKey, $expected, $realValue)) {
            return ["error" => "You've provided the wrong value for the given key!"];
        }
        
        TemplateManager::getInstance()->editTemplate(
            $template,
            ($editKey == "lobby" ? $realValue : null),
            ($editKey == "maintenance" ? $realValue : null),
            ($editKey == "static" ? $realValue : null),
            ($editKey == "maxPlayerCount" ? $realValue : null),
            ($editKey == "minServerCount" ? $realValue : null),
            ($editKey == "maxServerCount" ? $realValue : null),
            ($editKey == "startNewWhenFull" ? $realValue : null),
            ($editKey == "autoStart" ? $realValue : null)
        );
        return ["success" => "The template was edited!"];
    }

    public function isBadRequest(Request $request): bool {
        if ($request->data()->queries()->has("name") && $request->data()->queries()->has("key") && $request->data()->queries()->has("value")) return false;
        return true;
    }
}