<?php

namespace pocketcloud\plugin\loader;

use pocketcloud\plugin\CloudPlugin;
use pocketcloud\plugin\CloudPluginDescription;
use pocketcloud\util\Utils;

class FolderCloudPluginLoader implements CloudPluginLoader {

    public function canLoad(string $path): bool {
        return is_dir($path) && file_exists($path . "/plugin.yml") && file_exists($path . "/src/");
    }

    public function loadPlugin(string $path): string|CloudPlugin {
        $pluginYml = yaml_parse(file_get_contents($path . "/plugin.yml"));
        if (!is_array($pluginYml)) return "Can't parse plugin.yml";
        $pluginYml = CloudPluginDescription::fromArray($pluginYml);
        if ($pluginYml === null) return "Incorrect plugin.yml";

        Utils::requireDirectory($path . "/src");
        $plugin = new ($pluginYml->getMain())($pluginYml);
        if (!is_subclass_of($plugin, CloudPlugin::class)) return "Is not a valid CloudPlugin";
        return $plugin;
    }
}