<?php

namespace pocketcloud\cloud\http\endpoint\impl\cloud;

use pocketcloud\cloud\http\io\Request;
use pocketcloud\cloud\http\io\Response;
use pocketcloud\cloud\http\util\Router;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginManager;
use pocketcloud\cloud\http\endpoint\EndPoint;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\util\VersionInfo;

class CloudInfoEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/cloud/info/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $templates = array_map(fn(Template $template) => $template->getName(), TemplateManager::getInstance()->getAll());
        $runningServers = array_map(fn(CloudServer $cloudServer) => $cloudServer->getName(), CloudServerManager::getInstance()->getAll());
        $loadedPlugins = array_map(fn(CloudPlugin $plugin) => $plugin->getDescription()->getName(), CloudPluginManager::getInstance()->getAll());
        $enabledPlugins = array_map(fn(CloudPlugin $plugin) => $plugin->getDescription()->getName(), CloudPluginManager::getInstance()->getEnabled());
        $disabledPlugins = array_filter($loadedPlugins, fn(string $name) => !in_array($name, $enabledPlugins));
        $players = array_map(fn(CloudPlayer $player) => $player->getName(), CloudPlayerManager::getInstance()->getAll());

        return [
            "version" => VersionInfo::VERSION,
            "developer" => VersionInfo::DEVELOPERS,
            "templates" => array_values($templates),
            "runningServers" => array_values($runningServers),
            "players" => array_values($players),
            "loadedPlugins" => array_values($loadedPlugins),
            "enabledPlugins" => array_values($enabledPlugins),
            "disabledPlugins" => array_values($disabledPlugins),
            "network_address" => Network::getInstance()->getAddress()->__toString()
        ];
    }

    public function isBadRequest(Request $request): bool {
        return false;
    }
}