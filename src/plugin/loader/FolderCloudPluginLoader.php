<?php

namespace pocketcloud\cloud\plugin\loader;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginDescription;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\PLUGINS_PATH;

final class FolderCloudPluginLoader implements CloudPluginLoader {

    public function canLoad(string $path): bool {
        return is_dir($path) && file_exists($path . "/plugin.yml") && file_exists($path . "/src/");
    }

    public function loadPlugin(string $path): string|CloudPlugin {
        $pluginYml = FileUtils::parseYamlFile($path . "/plugin.yml");
        CloudLogger::get()->debug("Parsing plugin.yml... (" . $path . ")");
        if (!is_array($pluginYml)) return "Failed to parse plugin.yml";
        $pluginYml = CloudPluginDescription::read($pluginYml);
        if ($pluginYml === null) return "Invalid plugin.yml";

        CloudLogger::get()->debug("Adding plugin to class loader (" . $path . ")");
        Server::getInstance()->getClassLoader()->addPrefix($pluginYml->getSrcNamespacePrefix(), $path . "/src");
        $plugin = new ($pluginYml->getMain())($pluginYml, PathUtils::join(PLUGINS_PATH, strtolower($pluginYml->getName())) . "/", $path);
        if (!is_subclass_of($plugin, CloudPlugin::class)) return "Is not a valid CloudPlugin";
        return $plugin;
    }
}