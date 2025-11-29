<?php

namespace pocketcloud\cloud\plugin;

use pocketcloud\cloud\event\EventManager;
use pocketcloud\cloud\event\impl\plugin\PluginDisableEvent;
use pocketcloud\cloud\event\impl\plugin\PluginEnableEvent;
use pocketcloud\cloud\event\impl\plugin\PluginLoadEvent;
use pocketcloud\cloud\plugin\loader\CloudPluginLoader;
use pocketcloud\cloud\plugin\loader\FolderCloudPluginLoader;
use pocketcloud\cloud\plugin\loader\PharCloudPluginLoader;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\tick\Tickable;
use Throwable;

final class CloudPluginManager implements Tickable {
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

    public function loadAll(): void {
        CloudLogger::get()->debug("Loading plugins...");
        foreach (array_diff(scandir(CLOUD_PLUGINS_PATH), [".", ".."]) as $file) {
            $path = CLOUD_PLUGINS_PATH . $file;
            $this->load($path);
        }

        if (count($this->plugins) == 0) {
            CloudLogger::get()->info("No plugins were loaded.");
        } else {
            CloudLogger::get()->success("Successfully loaded §b" . count($this->plugins) . " plugin" . (count($this->plugins) == 1 ? "" : "s") . "§r.");
        }
    }

    public function load(string $path): void {
        CloudLogger::get()->debug("Loading plugim §b" . basename($path) . "§r...");
        foreach ($this->loaders as $loader) {
            try {
                if ($loader->canLoad($path)) {
                    $plugin = $loader->loadPlugin($path);
                    if (!$plugin instanceof CloudPlugin) {
                        CloudLogger::get()->error("§cFailed to load the plugin §e" . basename($path) . "§c: §eMain Class does not inherit from §eCloudPlugin");
                        return;
                    }

                    if (isset($this->plugins[$plugin->getDescription()->getName()])) {
                        CloudLogger::get()->warn("§cThe plugin §e" . $plugin->getDescription()->getName() . " §cis already loaded.");
                        return;
                    }

                    new PluginLoadEvent($plugin)->call();
                    $this->plugins[$plugin->getDescription()->getName()] = $plugin;
                    $plugin->onLoad();
                }
            } catch (Throwable $exception) {
                CloudLogger::get()->error("§cFailed to load the plugin §e" . basename($path) . "§c: §e" . $exception->getMessage());
                CloudLogger::get()->exception($exception);
            }
        }
    }

    public function enableAll(): void {
        CloudLogger::get()->debug("Enabling plugins...");
        foreach ($this->plugins as $plugin) {
            $this->enable($plugin);
        }

        if (count($this->enabledPlugins) > 0) {
            CloudLogger::get()->success("Successfully enabled §b" . count($this->enabledPlugins) . " plugin" . (count($this->enabledPlugins) == 1 ? "" : "s") . "§r.");
        }
    }

    public function enable(CloudPlugin $plugin): void {
        CloudLogger::get()->info("Enabling §b" . $plugin->getDescription()->getName() . "§r...");
        $plugin->setEnabled(true);
        new PluginEnableEvent($plugin)->call();
        try {
            $plugin->onEnable();
        } catch (Throwable $throwable) {
            CloudLogger::get()->exception($throwable);
            $this->disable($plugin);
        }

        if ($plugin->isEnabled()) {
            $this->enabledPlugins[$plugin->getDescription()->getName()] = $plugin;
        }
    }

    public function disableAll(): void {
        foreach ($this->enabledPlugins as $plugin) {
            $this->disable($plugin);
        }

        CloudLogger::get()->info("Disabled all plugins.");
    }

    public function disable(CloudPlugin $plugin): void {
        CloudLogger::get()->info("Disabling §b" . $plugin->getDescription()->getName() . "§r...");
        new PluginDisableEvent($plugin)->call();
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

    public function tick(int $currentTick): void {
        foreach ($this->enabledPlugins as $enabledPlugin) {
            if ($enabledPlugin->isEnabled()) {
                $enabledPlugin->getScheduler()->tick($currentTick);
            }
        }
    }

    public function get(string $name): ?CloudPlugin {
        return $this->plugins[$name] ?? null;
    }

    public function getLoaders(): array {
        return $this->loaders;
    }

    public function getAll(bool $enabled = false): array {
        return $enabled ? $this->enabledPlugins : $this->plugins;
    }
}