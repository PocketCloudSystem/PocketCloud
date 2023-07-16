<?php

namespace pocketcloud\plugin;

use pocketcloud\event\EventManager;
use pocketcloud\event\impl\plugin\PluginDisableEvent;
use pocketcloud\event\impl\plugin\PluginEnableEvent;
use pocketcloud\event\impl\plugin\PluginLoadEvent;
use pocketcloud\language\Language;
use pocketcloud\plugin\loader\FolderCloudPluginLoader;
use pocketcloud\plugin\loader\PharCloudPluginLoader;
use pocketcloud\plugin\loader\CloudPluginLoader;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;
use pocketcloud\util\Tickable;

class CloudPluginManager implements Tickable, Reloadable {
    use SingletonTrait;

    /** @var array<CloudPlugin> */
    private array $plugins = [];
    /** @var array<CloudPlugin> */
    private array $enabledPlugins = [];
    /** @var array<CloudPluginLoader> */
    private array $loaders = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerLoader(new PharCloudPluginLoader());
        $this->registerLoader(new FolderCloudPluginLoader());
    }

    public function registerLoader(CloudPluginLoader $loader): void {
        $this->loaders[] = $loader;
    }

    public function loadPlugins(): void {
        CloudLogger::get()->info(Language::current()->translate("plugins.loading"));
        foreach (array_diff(scandir(CLOUD_PLUGINS_PATH), [".", ".."]) as $file) {
            $path = CLOUD_PLUGINS_PATH . $file;
            $this->loadPlugin($path);
        }

        if (count($this->plugins) == 0) {
            CloudLogger::get()->info(Language::current()->translate("plugins.loaded.none"));
        } else {
            CloudLogger::get()->info(Language::current()->translate("plugins.loaded", count($this->plugins)));
        }
    }

    public function loadPlugin(string $path): void {
        CloudLogger::get()->info(Language::current()->translate("plugin.load", basename($path)));
        foreach ($this->loaders as $loader) {
            try {
                if ($loader->canLoad($path)) {
                    $plugin = $loader->loadPlugin($path);
                    if (!$plugin instanceof CloudPlugin) {
                        CloudLogger::get()->error(Language::current()->translate("plugin.loading.failed", basename($path), $plugin));
                        return;
                    }

                    if (isset($this->plugins[$plugin->getDescription()->getName()])) {
                        CloudLogger::get()->error(Language::current()->translate("plugin.loading.failed", $plugin->getDescription()->getName(), "Plugin already exists"));
                        return;
                    }

                    (new PluginLoadEvent($plugin))->call();
                    $this->plugins[$plugin->getDescription()->getName()] = $plugin;
                    $plugin->onLoad();
                }
            } catch (\Throwable $exception) {
                CloudLogger::get()->error(Language::current()->translate("plugin.loading.failed", basename($path), $exception->getMessage()));
                CloudLogger::get()->exception($exception);
            }
        }
    }

    public function enablePlugins(): void {
        CloudLogger::get()->info(Language::current()->translate("plugins.enabling"));
        foreach ($this->plugins as $plugin) {
            $this->enablePlugin($plugin);
        }

        if (count($this->enabledPlugins) == 0) {
            CloudLogger::get()->info(Language::current()->translate("plugins.enabled.none"));
        } else {
            CloudLogger::get()->info(Language::current()->translate("plugins.enabled", count($this->enabledPlugins)));
        }
    }

    public function enablePlugin(CloudPlugin $plugin): void {
        CloudLogger::get()->info(Language::current()->translate("plugin.enabling", $plugin->getDescription()->getName(), $plugin->getDescription()->getFullName()));
        $plugin->setEnabled(true);
        (new PluginEnableEvent($plugin))->call();
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

    public function disablePlugins(): void {
        foreach ($this->enabledPlugins as $plugin) {
            $this->disablePlugin($plugin);
        }

        CloudLogger::get()->info(Language::current()->translate("plugins.disabled"));
    }

    public function disablePlugin(CloudPlugin $plugin): void {
        CloudLogger::get()->info(Language::current()->translate("plugin.disabling", $plugin->getDescription()->getName(), $plugin->getDescription()->getFullName()));
        (new PluginDisableEvent($plugin))->call();
        $plugin->setEnabled(false);
        $plugin->onDisable();

        $plugin->getScheduler()->cancelAll();
        EventManager::getInstance()->removeHandlers($plugin);
        if (isset($this->enabledPlugins[$plugin->getDescription()->getName()])) unset($this->enabledPlugins[$plugin->getDescription()->getName()]);
    }

    public function clear(): void {
        $this->plugins = [];
        $this->enabledPlugins = [];
    }

    public function reload(): bool {
        foreach ($this->plugins as $plugin) {
            if ($plugin->isEnabled()) $this->disablePlugin($plugin);
            unset($this->plugins[$plugin->getDescription()->getName()]);
        }

        $this->clear();

        foreach (array_diff(scandir(CLOUD_PLUGINS_PATH), [".", ".."]) as $file) {
            $path = CLOUD_PLUGINS_PATH . $file;
            $this->loadPlugin($path);
        }
        return true;
    }

    public function tick(int $currentTick): void {
        foreach ($this->enabledPlugins as $enabledPlugin) {
            if ($enabledPlugin->isEnabled()) {
                $enabledPlugin->getScheduler()->tick($currentTick);
            }
        }
    }

    public function getPluginByName(string $name): ?CloudPlugin {
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

    public static function getInstance(): ?self {
        return self::$instance;
    }
}