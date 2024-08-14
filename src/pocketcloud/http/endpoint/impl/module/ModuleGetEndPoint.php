<?php

namespace pocketcloud\http\endpoint\impl\module;

use pocketcloud\config\impl\ModuleConfig;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;

class ModuleGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/module/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $module = strtolower($request->data()->queries()->get("module"));

        if (in_array($module, ["sign", "signmodule", "cloudsigns"])) {
            return ["module" => "signModule", "enabled" => ModuleConfig::getInstance()->isSignModule()];
        } else if (in_array($module, ["npc", "npcmodule", "cloudnpcs"])) {
            return ["module" => "npcModule", "enabled" => ModuleConfig::getInstance()->isNpcModule()];
        } else if (in_array($module, ["hub", "hubcommand", "hubcommandmodule"])) {
            return ["module" => "hubCommandModule", "enabled" => ModuleConfig::getInstance()->isHubCommandModule()];
        }

        return ["error" => "The module doesn't exists!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("module");
    }
}