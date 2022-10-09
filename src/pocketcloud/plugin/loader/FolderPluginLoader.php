<?php

namespace pocketcloud\plugin\loader;

use pocketcloud\plugin\Plugin;
use pocketcloud\plugin\PluginDescription;
use pocketcloud\utils\Utils;

class FolderPluginLoader implements PluginLoader {

    public function canLoad(string $path): bool {
        return is_dir($path) && file_exists($path . "/plugin.yml") && file_exists($path . "/src/");
    }

    public function loadPlugin(string $path): string|Plugin {
        $pluginYml = @yaml_parse(file_get_contents($path . "/plugin.yml"));
        if (!is_array($pluginYml)) return "Can't parse plugin.yml";
        $pluginYml = PluginDescription::fromArray($pluginYml);
        if ($pluginYml === null) return "Incorrect plugin.yml";

        Utils::requireAll($path . "/src");
        $plugin = new ($pluginYml->getMain())($pluginYml);
        if (!is_subclass_of($plugin, Plugin::class)) return "Doesn't extend from Plugin class";
        return $plugin;
    }
}