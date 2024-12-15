<?php

namespace pocketcloud\cloud\plugin\loader;

use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginDescription;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;

final class FolderCloudPluginLoader implements CloudPluginLoader {

    public function canLoad(string $path): bool {
        return is_dir($path) && file_exists($path . "/plugin.yml") && file_exists($path . "/src/");
    }

    public function loadPlugin(string $path): string|CloudPlugin {
        $pluginYml = yaml_parse(file_get_contents($path . "/plugin.yml"));
        CloudLogger::get()->debug("Parsing plugin.yml... (" . $path . ")");
        if (!is_array($pluginYml)) return "Can't parse plugin.yml";
        $pluginYml = CloudPluginDescription::fromArray($pluginYml);
        if ($pluginYml === null) return "Incorrect plugin.yml";

        CloudLogger::get()->debug("Adding plugin to class loader (" . $path . ")");
        PocketCloud::getInstance()->getClassLoader()->addPath("", $path . "/src");
        $plugin = new ($pluginYml->getMain())($pluginYml);
        if (!is_subclass_of($plugin, CloudPlugin::class)) return "Is not a valid CloudPlugin";
        return $plugin;
    }
}