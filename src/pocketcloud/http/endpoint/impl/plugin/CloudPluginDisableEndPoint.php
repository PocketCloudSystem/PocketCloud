<?php

namespace pocketcloud\http\endpoint\impl\plugin;

use pocketcloud\http\io\Request;
use pocketcloud\http\io\Response;
use pocketcloud\http\util\Router;
use pocketcloud\plugin\CloudPluginManager;
use pocketcloud\http\endpoint\EndPoint;

class CloudPluginDisableEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::POST, "/plugin/disable/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $plugin = CloudPluginManager::getInstance()->getPluginByName($request->data()->queries()->get("plugin"));
        if ($plugin === null) {
            return ["error" => "Plugin wasn't found!"];
        }

        if ($plugin->isDisabled()) {
            return ["error" => "Plugin is already disabled!"];
        }

        CloudPluginManager::getInstance()->disablePlugin($plugin);
        return ["success" => "Plugin was disabled!"];
    }

    public function isBadRequest(Request $request): bool {
        return !$request->data()->queries()->has("plugin");
    }
}