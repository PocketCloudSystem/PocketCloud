<?php

namespace pocketcloud\cloud\plugin;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\event\EventManager;
use pocketcloud\cloud\event\impl\plugin\PluginDisableEvent;
use pocketcloud\cloud\event\impl\plugin\PluginEnableEvent;
use pocketcloud\cloud\event\impl\plugin\PluginLoadEvent;
use pocketcloud\cloud\plugin\loader\CloudPluginLoader;
use pocketcloud\cloud\plugin\loader\FolderCloudPluginLoader;
use pocketcloud\cloud\plugin\loader\PharCloudPluginLoader;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Throwable;
use const pocketcloud\PLUGINS_PATH;

final class CloudPluginManager implements Tickable, Loadable {
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

    public function load(): void {
        CloudLogger::get()->debug("Loading plugins...");
        foreach (array_diff(scandir(PLUGINS_PATH), [".", ".."]) as $file) {
            $path = PLUGINS_PATH . $file;
            $this->loadPlugin($path);
        }

        if (count($this->plugins) == 0) {
            CloudLogger::get()->info("No plugins were loaded.");
        } else {
            CloudLogger::get()->success("Successfully loaded §b" . count($this->plugins) . " plugin" . (count($this->plugins) == 1 ? "" : "s") . "§r.");
        }
    }

    public function loadPlugin(string $path): void {
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
            $this->enablePlugin($plugin);
        }

        if (count($this->enabledPlugins) > 0) {
            CloudLogger::get()->success("Successfully enabled §b" . count($this->enabledPlugins) . " plugin" . (count($this->enabledPlugins) == 1 ? "" : "s") . "§r.");
        }
    }

    public function enablePlugin(CloudPlugin $plugin): void {
        CloudLogger::get()->info("Enabling §b" . $plugin->getDescription()->getName() . "§r...");
        $plugin->setEnabled(true);
        new PluginEnableEvent($plugin)->call();
        try {
            $plugin->onEnable();
        } catch (Throwable $throwable) {
            CloudLogger::get()->exception($throwable);
            $this->disablePlugin($plugin);
        }

        if ($plugin->isEnabled()) {
            $this->enabledPlugins[$plugin->getDescription()->getName()] = $plugin;
        }
    }

    public function disableAll(): void {
        foreach ($this->enabledPlugins as $plugin) {
            $this->disablePlugin($plugin);
        }

        CloudLogger::get()->info("Disabled all plugins.");
    }

    public function disablePlugin(CloudPlugin $plugin): void {
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