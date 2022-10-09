<?php

namespace pocketcloud\plugin\loader;

use pocketcloud\plugin\Plugin;

interface PluginLoader {

    public function canLoad(string $path): bool;

    public function loadPlugin(string $path): string|Plugin;
}