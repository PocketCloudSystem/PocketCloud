<?php

namespace pocketcloud\cloud\plugin\loader;

use Phar;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginDescription;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\PLUGINS_PATH;

final class PharCloudPluginLoader implements CloudPluginLoader {

    public function canLoad(string $path): bool {
        if (is_file($path) && pathinfo($path, PATHINFO_EXTENSION) == "phar") {
            $phar = new Phar($path);
            return isset($phar["plugin.yml"]) && isset($phar["src"]);
        }
        return false;
    }

    public function loadPlugin(string $path): string|CloudPlugin {
        $phar = new Phar($path);
        $pluginYml = FileUtils::parseYamlFile($phar["plugin.yml"]->getPathname());
        CloudLogger::get()->debug("Parsing plugin.yml... (" . $path . ")");
        if (!is_array($pluginYml)) return "Failed to parse plugin.yml";
        $pluginYml = CloudPluginDescription::read($pluginYml);
        if ($pluginYml === null) return "Invalid plugin.yml";

        CloudLogger::get()->debug("Adding plugin to class loader (" . $path . ")");
        PocketCloud::getInstance()->getClassLoader()->addPrefix($pluginYml->getSrcNamespacePrefix(), "phar://" . $path . "/src/");
        $plugin = new ($pluginYml->getMain())($pluginYml, PathUtils::join(PLUGINS_PATH, strtolower($pluginYml->getName())) . "/", $phar->getPath());
        if (!is_subclass_of($plugin, CloudPlugin::class)) return "Is not a valid CloudPlugin";
        return $plugin;
    }
}