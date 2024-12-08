<?php

namespace pocketcloud\cloud\http\endpoint\impl\plugin;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\plugin\CloudPluginManager;
use pocketcloud\cloud\http\endpoint\EndPoint;

final class CloudPluginGetEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/plugin/get");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = CloudPluginManager::getInstance()->get($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        return array_merge($plugin->getDescription()->toArray(), ["enabled" => $plugin->isEnabled()]);
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}