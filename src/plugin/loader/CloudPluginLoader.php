<?php

namespace pocketcloud\cloud\plugin\loader;

use pocketcloud\cloud\plugin\CloudPlugin;

interface CloudPluginLoader {

    public function canLoad(string $path): bool;

    public function loadPlugin(string $path): string|CloudPlugin;
}