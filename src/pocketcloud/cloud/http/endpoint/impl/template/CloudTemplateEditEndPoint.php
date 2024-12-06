<?php

namespace pocketcloud\cloud\http\endpoint\impl\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\template\TemplateManager;

class CloudTemplateEditEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::PATCH, "/template/edit/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $template = TemplateManager::getInstance()->get($name);

        if ($template === null) {
            return ["error" => "The template doesn't exists!"];
        }

        $localTemplateData = $template->toArray();
        foreach ($request->data()->queries()->all() as $key => $value) {
            if (TemplateHelper::isValidEditKey($key) && TemplateHelper::isValidEditValue($value, $key, $expected, $realValue)) {
                $localTemplateData[$key] = $realValue;
            }
        }
        
        TemplateManager::getInstance()->edit(
            $template,
            $localTemplateData["lobby"],
            $localTemplateData["maintenance"],
            $localTemplateData["static"],
            $localTemplateData["maxPlayerCount"],
            $localTemplateData["minServerCount"],
            $localTemplateData["maxServerCount"],
            $localTemplateData["startNewWhenFull"],
            $localTemplateData["autoStart"]
        );
        return ["success" => "The template was edited!"];
    }

    public function isBadRequest(Request $request): bool {
        $atLeastOne = false;
        foreach (TemplateHelper::EDITABLE_KEYS as $key) if ($request->data()->queries()->has($key)) {
            $atLeastOne = true;
            break;
        }

        if ($request->data()->queries()->has("name") && $atLeastOne) return false;
        return true;
    }
}