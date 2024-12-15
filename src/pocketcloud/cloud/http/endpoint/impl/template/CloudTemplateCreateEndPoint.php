<?php

namespace pocketcloud\cloud\http\endpoint\impl\template;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateSettings;
use pocketcloud\cloud\template\TemplateType;

final class CloudTemplateCreateEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/template/create/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $name = $request->data()->queries()->get("name");
        $type = $request->data()->queries()->has("type") ? (TemplateType::get($request->data()->queries()->get("type")) ?? TemplateType::SERVER()) : TemplateType::SERVER();
        $lobby = $this->bool($request->data()->queries()->get("lobby", "no"));
        $maintenance = $this->bool($request->data()->queries()->get("maintenance", "yes"));
        $static = $this->bool($request->data()->queries()->get("static", "no"));
        $maxPlayerCount = ($request->data()->queries()->has("maxPlayerCount") ? intval($request->data()->queries()->get("maxPlayerCount")) : 20);
        $minServerCount = ($request->data()->queries()->has("minServerCount") ? intval($request->data()->queries()->get("minServerCount")) : 0);
        $maxServerCount = ($request->data()->queries()->has("maxServerCount") ? intval($request->data()->queries()->get("maxServerCount")) : 2);
        $startNewPercentage = ($request->data()->queries()->has("startNewPercentage") ? floatval($request->data()->queries()->get("startNewPercentage")) : 0);
        $autoStart = $this->bool($request->data()->queries()->get("autoStart"));
        if ($maxPlayerCount < 0) $maxPlayerCount = 20;
        if ($minServerCount < 0) $minServerCount = 0;
        if ($maxServerCount < 0) $maxServerCount = 2;

        if (TemplateManager::getInstance()->get($name) !== null) {
            return ["error" => "The template already exists!"];
        }

        TemplateManager::getInstance()->create(Template::create($name, TemplateSettings::create($lobby, $maintenance, $static, $maxPlayerCount, $minServerCount, $maxServerCount, $startNewPercentage, $autoStart), $type));
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