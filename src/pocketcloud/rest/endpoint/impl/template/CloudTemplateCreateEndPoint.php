<?php

namespace pocketcloud\rest\endpoint\impl\template;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;

class CloudTemplateCreateEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/template/create/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $type = $request->data()->queries()->has("type") ? (TemplateType::getTemplateTypeByName($request->data()->queries()->get("type")) ?? TemplateType::SERVER()) : TemplateType::SERVER();
        $lobby = $this->bool($request->data()->queries()->get("lobby"));
        $maintenance = $this->bool($request->data()->queries()->get("maintenance"));
        $static = $this->bool($request->data()->queries()->get("static"));
        $maxPlayerCount = ($request->data()->queries()->has("maxPlayerCount") ? intval($request->data()->queries()->get("maxPlayerCount")) : 20);
        $minServerCount = ($request->data()->queries()->has("minServerCount") ? intval($request->data()->queries()->get("minServerCount")) : 0);
        $maxServerCount = ($request->data()->queries()->has("maxServerCount") ? intval($request->data()->queries()->get("maxServerCount")) : 2);
        $startNewWhenFull = $this->bool($request->data()->queries()->get("startNewWhenFull"));
        $autoStart = $this->bool($request->data()->queries()->get("autoStart"));
        if ($maxPlayerCount < 0) $maxPlayerCount = 20;
        if ($minServerCount < 0) $minServerCount = 0;
        if ($maxServerCount < 0) $maxServerCount = 2;

        if (TemplateManager::getInstance()->getTemplateByName($name) !== null) {
            return ["error" => "The template already exists!"];
        }

        TemplateManager::getInstance()->createTemplate(new Template($name, $lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewWhenFull, $autoStart, $type));
        return ["success" => "The template was successfully created!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("name");
    }

    private function bool(string $value): bool {
        if ($value == "true" || $value == "on" || $value == "yes") return true;
        return false;
    }
}