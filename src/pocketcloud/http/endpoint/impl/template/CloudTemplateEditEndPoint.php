<?php

namespace pocketcloud\http\endpoint\impl\template;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\template\TemplateHelper;
use pocketcloud\template\TemplateManager;

class CloudTemplateEditEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::PATCH, "/template/edit/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $template = TemplateManager::getInstance()->getTemplateByName($name);

        if ($template === null) {
            return ["error" => "The template doesn't exists!"];
        }

        $localTemplateData = $template->toArray();
        foreach ($request->data()->queries()->all() as $key => $value) {
            if (TemplateHelper::isValidEditKey($key) && TemplateHelper::isValidEditValue($value, $key, $expected, $realValue)) {
                $localTemplateData[$key] = $realValue;
            }
        }
        
        TemplateManager::getInstance()->editTemplate(
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