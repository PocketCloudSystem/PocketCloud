<?php

namespace pocketcloud\http\endpoint\impl\plugin;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\plugin\CloudPluginManager;
use pocketcloud\http\endpoint\EndPoint;

class CloudPluginGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/plugin/get");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = CloudPluginManager::getInstance()->getPluginByName($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        return array_merge($plugin->getDescription()->toArray(), ["enabled" => $plugin->isEnabled()]);
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}