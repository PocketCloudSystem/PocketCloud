<?php

namespace pocketcloud\cloud\http\endpoint\impl\module;

use pocketcloud\cloud\cache\InGameModule;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;

class ModuleGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/module/get/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $module = strtolower($request->data()->queries()->get("module"));

        if (in_array($module, ["sign", "signmodule", "cloudsigns"])) {
            return ["module" => "signModule", "enabled" => InGameModule::getModuleState(InGameModule::SIGN_MODULE)];
        } else if (in_array($module, ["npc", "npcmodule", "cloudnpcs"])) {
            return ["module" => "npcModule", "enabled" => InGameModule::getModuleState(InGameModule::NPC_MODULE)];
        } else if (in_array($module, ["hub", "hubcommand", "hubcommandmodule"])) {
            return ["module" => "hubCommandModule", "enabled" => InGameModule::getModuleState(InGameModule::HUB_COMMAND_MODULE)];
        }

        return ["error" => "The module doesn't exists!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("module");
    }
}