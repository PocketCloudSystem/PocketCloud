<?php

declare(strict_types=1);

namespace pocketcloud\cloud;

use pocketcloud\cloud\util\PathUtils;

if (defined('pocketcloud\_CORE_CONSTANTS_INCLUDED')) {
    return;
}

define('pocketcloud\_CORE_CONSTANTS_INCLUDED', true);
define("pocketcloud\VENDOR_AUTOLOAD_PATH", dirname(__FILE__, 2) . "/vendor/autoload.php");
define("pocketcloud\IS_PHAR", Phar::running() !== "");
define("pocketcloud\SOURCE_PATH", __DIR__ . "/");

define("pocketcloud\CLOUD_PATH", (\pocketcloud\IS_PHAR ?
    str_replace("phar://", "", dirname(__DIR__, 2) . "/") :
    dirname(__DIR__) . "/"
));

define("pocketcloud\STORAGE_PATH", PathUtils::join(\pocketcloud\CLOUD_PATH, "storage") . "/");
define("pocketcloud\BACKUPS_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "backups") . "/");
define("pocketcloud\INTERNAL_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "internal") . "/");
define("pocketcloud\CRASHES_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "crashes") . "/");
define("pocketcloud\SERVER_CRASHES_PATH", PathUtils::join(\pocketcloud\CRASHES_PATH, "servers") . "/");
define("pocketcloud\BINARIES_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "binaries") . "/");
define("pocketcloud\LIBRARIES_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "libraries") . "/");
define("pocketcloud\PLUGINS_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "plugins") . "/");
define("pocketcloud\SOFTWARE_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "software") . "/");
define("pocketcloud\IN_GAME_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "inGame") . "/");
define("pocketcloud\STATIC_SERVERS_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "staticServers") . "/");
define("pocketcloud\LOG_PATH", PathUtils::join(\pocketcloud\STORAGE_PATH, "cloud.log"));
define("pocketcloud\TEMP_PATH", PathUtils::join(\pocketcloud\CLOUD_PATH, "tmp") . "/");
define("pocketcloud\TEMPLATES_PATH", PathUtils::join(\pocketcloud\CLOUD_PATH, "templates") . "/");
define("pocketcloud\GLOBAL_TEMPLATES_PATH", PathUtils::join(\pocketcloud\TEMPLATES_PATH, "global") . "/");
define("pocketcloud\SERVER_GROUPS_PATH", PathUtils::join(\pocketcloud\CLOUD_PATH, "groups") . "/");
define("pocketcloud\FIRST_RUN", !file_exists(\pocketcloud\STORAGE_PATH . "config.json"));