<?php

namespace pocketcloud\plugin\loader;

use pocketcloud\plugin\Plugin;
use pocketcloud\plugin\PluginDescription;
use pocketcloud\utils\Utils;

class PharPluginLoader implements PluginLoader {

    public function canLoad(string $path): bool {
        if (is_file($path) && pathinfo($path, PATHINFO_EXTENSION) == "phar") {
            $phar = new \Phar($path);
            return isset($phar["plugin.yml"]) && isset($phar["src"]);
        }
        return false;
    }

    public function loadPlugin(string $path): string|Plugin {
        $phar = new \Phar($path);
        $pluginYml = @yaml_parse(file_get_contents($phar["plugin.yml"]->getPathname()));
        if ($pluginYml == false) return "Can't parse plugin.yml";
        $pluginYml = PluginDescription::fromArray($pluginYml);
        if ($pluginYml === null) return "Incorrect plugin.yml";

        Utils::requireAll("phar://" . $path . "/src");
        $plugin = new ($pluginYml->getMain())($pluginYml);
        if (!is_subclass_of($plugin, Plugin::class)) return "Doesn't extend from Plugin class";
        return $plugin;
    }
}