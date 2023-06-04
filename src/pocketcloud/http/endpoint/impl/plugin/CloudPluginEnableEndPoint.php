<?php

namespace pocketcloud\http\endpoint\impl\plugin;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\endpoint\EndPoint;
use pocketcloud\http\util\Router;
use pocketcloud\plugin\CloudPluginManager;

class CloudPluginEnableEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/plugin/enable/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = CloudPluginManager::getInstance()->getPluginByName($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        if ($plugin->isEnabled()) {
            return ["error" => "Plugin is already enabled!"];
        }

        CloudPluginManager::getInstance()->enablePlugin($plugin);
        return ["success" => "Plugin was enabled!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}