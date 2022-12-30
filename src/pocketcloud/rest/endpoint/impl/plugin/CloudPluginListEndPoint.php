<?php

namespace pocketcloud\rest\endpoint\impl\plugin;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\plugin\Plugin;
use pocketcloud\plugin\PluginManager;
use pocketcloud\rest\endpoint\EndPoint;

class CloudPluginListEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/plugin/list/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $loadedPlugins = array_map(fn(Plugin $plugin) => $plugin->getDescription()->getName(), PluginManager::getInstance()->getPlugins());
        $enabledPlugins = array_map(fn(Plugin $plugin) => $plugin->getDescription()->getName(), PluginManager::getInstance()->getEnabledPlugins());
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