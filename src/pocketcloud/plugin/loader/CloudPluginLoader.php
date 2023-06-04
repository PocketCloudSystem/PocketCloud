<?php

namespace pocketcloud\plugin\loader;

use pocketcloud\plugin\CloudPlugin;

interface CloudPluginLoader {

    public function canLoad(string $path): bool;

    public function loadPlugin(string $path): string|CloudPlugin;
}