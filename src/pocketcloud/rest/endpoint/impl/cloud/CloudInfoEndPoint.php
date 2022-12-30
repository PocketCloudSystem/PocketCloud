<?php

namespace pocketcloud\rest\endpoint\impl\cloud;

use pocketcloud\lib\express\io\Request;
use pocketcloud\lib\express\io\Response;
use pocketcloud\lib\express\route\Router;
use pocketcloud\network\Network;
use pocketcloud\player\CloudPlayer;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\plugin\Plugin;
use pocketcloud\plugin\PluginManager;
use pocketcloud\rest\endpoint\EndPoint;
use pocketcloud\server\CloudServer;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\utils\VersionInfo;

class CloudInfoEndPoint extends EndPoint {

    public function __construct() {
        parent::__construct(Router::GET, "/cloud/info/");
    }

    public function handleRequest(Request $request, Response $response): array {
        $templates = array_map(fn(Template $template) => $template->getName(), TemplateManager::getInstance()->getTemplates());
        $runningServers = array_map(fn(CloudServer $cloudServer) => $cloudServer->getName(), CloudServerManager::getInstance()->getServers());
        $loadedPlugins = array_map(fn(Plugin $plugin) => $plugin->getDescription()->getName(), PluginManager::getInstance()->getPlugins());
        $enabledPlugins = array_map(fn(Plugin $plugin) => $plugin->getDescription()->getName(), PluginManager::getInstance()->getEnabledPlugins());
        $disabledPlugins = array_filter($loadedPlugins, fn(string $name) => !in_array($name, $enabledPlugins));
        $players = array_map(fn(CloudPlayer $player) => $player->getName(), CloudPlayerManager::getInstance()->getPlayers());

        return [
            "version" => VersionInfo::VERSION,
            "version_developer" => VersionInfo::DEVELOPERS,
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