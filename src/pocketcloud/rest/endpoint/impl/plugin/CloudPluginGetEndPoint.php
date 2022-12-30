<?php

namespace pocketcloud\rest\endpoint\impl\plugin;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\plugin\PluginManager;
use pocketcloud\rest\endpoint\EndPoint;

class CloudPluginGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/plugin/get");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = PluginManager::getInstance()->getPluginByName($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        return array_merge($plugin->getDescription()->toArray(), ["enabled" => $plugin->isEnabled()]);
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}