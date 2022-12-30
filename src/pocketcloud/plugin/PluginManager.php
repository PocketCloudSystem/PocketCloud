<?php

namespace pocketcloud\plugin;

use pocketcloud\event\impl\plugin\PluginDisableEvent;
use pocketcloud\event\impl\plugin\PluginEnableEvent;
use pocketcloud\event\impl\plugin\PluginLoadEvent;
use pocketcloud\plugin\loader\FolderPluginLoader;
use pocketcloud\plugin\loader\PharPluginLoader;
use pocketcloud\plugin\loader\PluginLoader;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;

class PluginManager {
    use SingletonTrait;

    /** @var array<Plugin> */
    private array $plugins = [];
    /** @var array<Plugin> */
    private array $enabledPlugins = [];
    /** @var array<PluginLoader> */
    private array $loaders = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerLoader(new PharPluginLoader());
        $this->registerLoader(new FolderPluginLoader());
    }

    public function registerLoader(PluginLoader $loader) {
        $this->loaders[] = $loader;
    }

    public function loadPlugins() {
        foreach (array_diff(scandir(CLOUD_PLUGINS_PATH), [".", ".."]) as $file) {
            $path = CLOUD_PLUGINS_PATH . $file;
            $this->loadPlugin($path);
        }

        CloudLogger::get()->info("Successfully loaded §e" . count($this->plugins) . " plugin" . (count($this->plugins) == 1 ? "" : "s") . "§r!");
    }

    public function loadPlugin(string $path) {
        foreach ($this->loaders as $loader) {
            if ($loader->canLoad($path)) {
                $plugin = $loader->loadPlugin($path);
                if (!$plugin instanceof Plugin) {
                    CloudLogger::get()->error("§cCan't load plugin §e" . basename($path) . "§c: §e" . $plugin);
                    return;
                }

                if (isset($this->plugins[$plugin->getDescription()->getName()])) {
                    CloudLogger::get()->error("§cCan't load plugin §e" . basename($path) . "§c: §ePlugin already exists");
                    return;
                }

                (new PluginLoadEvent($plugin))->call();
                CloudLogger::get()->info("Loading plugin " . $plugin->getDescription()->getName() . " (" . $plugin->getDescription()->getFullName() . ")");
                $this->plugins[$plugin->getDescription()->getName()] = $plugin;
                $plugin->onLoad();
            }
        }
    }

    public function enablePlugins() {
        foreach ($this->plugins as $plugin) {
            $this->enablePlugin($plugin);
        }

        CloudLogger::get()->info("All plugins were successfully enabled!");
    }

    public function enablePlugin(Plugin $plugin) {
        (new PluginEnableEvent($plugin))->call();
        CloudLogger::get()->info("Enabling plugin " . $plugin->getDescription()->getName() . " (" . $plugin->getDescription()->getFullName() . ")");
        $plugin->setEnabled(true);
        try {
            $plugin->onEnable();
        } catch (\Throwable $throwable) {
            CloudLogger::get()->exception($throwable);
            $this->disablePlugin($plugin);
        }

        if ($plugin->isEnabled()) {
            $this->enabledPlugins[$plugin->getDescription()->getName()] = $plugin;
        }
    }

    public function disablePlugins() {
        foreach ($this->enabledPlugins as $plugin) {
            $this->disablePlugin($plugin);
        }

        CloudLogger::get()->info("All plugins were successfully disabled!");
    }

    public function disablePlugin(Plugin $plugin) {
        (new PluginDisableEvent($plugin))->call();
        CloudLogger::get()->info("Disabling plugin " . $plugin->getDescription()->getName() . " (" . $plugin->getDescription()->getFullName() . ")");
        $plugin->setEnabled(false);
        $plugin->onDisable();

        if (isset($this->enabledPlugins[$plugin->getDescription()->getName()])) unset($this->enabledPlugins[$plugin->getDescription()->getName()]);
    }

    public function clear() {
        $this->plugins = [];
        $this->enabledPlugins = [];
    }

    public function reload() {
        foreach ($this->plugins as $plugin) {
            if ($plugin->isEnabled()) $this->disablePlugin($plugin);
            unset($this->plugins[$plugin->getDescription()->getName()]);
        }

        $this->clear();

        foreach (array_diff(scandir(CLOUD_PLUGINS_PATH), [".", ".."]) as $file) {
            $path = CLOUD_PLUGINS_PATH . $file;
            $this->loadPlugin($path);
        }
    }

    public function getPluginByName(string $name): ?Plugin {
        return $this->plugins[$name] ?? null;
    }

    public function getLoaders(): array {
        return $this->loaders;
    }

    public function getEnabledPlugins(): array {
        return $this->enabledPlugins;
    }

    public function getPlugins(): array {
        return $this->plugins;
    }
}