<?php

namespace pocketcloud\rest\endpoint\impl\plugin;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\plugin\PluginManager;
use pocketcloud\rest\endpoint\EndPoint;

class CloudPluginDisableEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/plugin/disable/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = PluginManager::getInstance()->getPluginByName($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        if ($plugin->isDisabled()) {
            return ["error" => "Plugin is already disabled!"];
        }

        PluginManager::getInstance()->disablePlugin($plugin);
        return ["success" => "Plugin was disabled!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}