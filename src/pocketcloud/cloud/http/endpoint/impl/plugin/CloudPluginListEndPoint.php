<?php

namespace pocketcloud\cloud\http\endpoint\impl\plugin;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginManager;
use pocketcloud\cloud\http\endpoint\EndPoint;

class CloudPluginListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/plugin/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $loadedPlugins = array_map(fn(CloudPlugin $plugin) => $plugin->getDescription()->getName(), CloudPluginManager::getInstance()->getAll());
        $enabledPlugins = array_map(fn(CloudPlugin $plugin) => $plugin->getDescription()->getName(), CloudPluginManager::getInstance()->getAll(true));
        $disabledPlugins = array_filter($loadedPlugins, fn(string $name) => !in_array($name, $enabledPlugins));
        return [
            "loadedPlugins" => array_values($loadedPlugins),
            "enabledPlugins" => array_values($enabledPlugins),
            "disabledPlugins" => array_values($disabledPlugins)
        ];
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}