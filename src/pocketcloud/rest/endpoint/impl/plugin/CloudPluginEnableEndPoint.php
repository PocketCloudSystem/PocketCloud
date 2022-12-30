<?php

namespace pocketcloud\rest\endpoint\impl\plugin;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\plugin\PluginManager;
use pocketcloud\rest\endpoint\EndPoint;

class CloudPluginEnableEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/plugin/enable/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = PluginManager::getInstance()->getPluginByName($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        if ($plugin->isEnabled()) {
            return ["error" => "Plugin is already enabled!"];
        }

        PluginManager::getInstance()->enablePlugin($plugin);
        return ["success" => "Plugin was enabled!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}