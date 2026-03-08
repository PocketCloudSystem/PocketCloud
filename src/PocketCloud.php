<?php

declare(strict_types=1);

namespace pocketcloud\cloud;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\thread\ThreadManager;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\loader\ClassLoader;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\TerminalUtils;
use const pocketcloud\INTERNAL_PATH;

error_reporting(-1);

checkPlatform();

foreach ([
             \pocketcloud\STORAGE_PATH, \pocketcloud\BACKUPS_PATH, INTERNAL_PATH, \pocketcloud\CRASHES_PATH, \pocketcloud\SERVER_CRASHES_PATH, \pocketcloud\BINARIES_PATH, \pocketcloud\LIBRARIES_PATH, \pocketcloud\PLUGINS_PATH, \pocketcloud\SOFTWARE_PATH, \pocketcloud\IN_GAME_PATH, \pocketcloud\LOG_PATH,
			 \pocketcloud\TEMP_PATH,
			 \pocketcloud\TEMPLATES_PATH, \pocketcloud\GLOBAL_TEMPLATES_PATH,
			 \pocketcloud\SERVER_GROUPS_PATH
         ] as $dir) {
    FileUtils::createDir($dir);
}

if (checkRunning($pid)) {
    die("[ERROR] PocketCloud is already running in a different process. (PID: $pid)" . PHP_EOL);
}

$classLoader = new ClassLoader();
$classLoader->init();

$lockFile = createLockFile();

$cloud = new Server($classLoader);
$cloud->start();

if (ThreadManager::getInstance()->stopAll() > 0) {
    CloudLogger::get()->warn("Some threads crashed while trying to stop them, force-kill of the process...");
    @ProcessUtils::kill(getmypid());
}

releaseLockFile($lockFile);

exit(0);

function checkRunning(?int &$pid = null): bool {
    if (!file_exists(INTERNAL_PATH)) return false;
    $file = fopen(INTERNAL_PATH . "cloud.lock", "a+b");
    if ($file === false) return false;
    if (!flock($file, LOCK_EX | LOCK_NB)) {
        flock($file, LOCK_SH);
        $processId = stream_get_contents($file);
        if (preg_match('/^\d+$/', $processId) === 1) $pid = $processId;
        return true;
    }

    return false;
}

function createLockFile() {
    $file = fopen(INTERNAL_PATH . "cloud.lock", "a+b");
    if ($file === false) throw new RuntimeException("Failed to create cloud.lock file");
    if (!flock($file, LOCK_EX | LOCK_NB)) flock($file, LOCK_SH);
    ftruncate($file, 0);
    fwrite($file, (string) getmypid());
    fflush($file);
    flock($file, LOCK_SH);
    return $file;
}

function releaseLockFile(mixed $lockFile): void {
    flock($lockFile, LOCK_UN);
    fclose($lockFile);
    unlink(INTERNAL_PATH . "cloud.lock");
}

function checkPlatform(): void {
    if (PHP_OS_FAMILY == "Windows") {
        die("[ERROR] You can't use PocketCloud on a windows machine." . PHP_EOL);
    }

    $messages = [];

    if (version_compare("8.4.0", PHP_VERSION) > 0) {
        $messages[] = "PHP >=8.4.0 is required, but you have PHP " . PHP_VERSION . ".";
    }

    foreach ([
                 "curl" => "cURL",
                 "date" => "Date",
                 "igbinary" => "igbinary",
                 "json" => "JSON",
                 "mbstring" => "Multibyte String",
                 "pcre" => "PCRE",
                 "phar" => "Phar",
                 "pmmpthread" => "pmmpthread",
                 "reflection" => "Reflection",
                 "sockets" => "Sockets",
                 "spl" => "SPL",
                 "yaml" => "YAML",
                 "zip" => "Zip",
                 "zlib" => "Zlib",
                 "pcntl" => "pcntl"
             ] as $extension => $displayName) {
        if (!extension_loaded($extension)) {
            $messages[] = "[ERROR] Missing $displayName ($extension) as a extension.";
        }
    }

    if (($pmmpThreadVersion = phpversion("pmmpthread")) !== false){
        if (version_compare($pmmpThreadVersion, "6.1.0") < 0 || version_compare($pmmpThreadVersion, "7.0.0") >= 0) {
            $messages[] = "[WARN] pmmpthread ^6.1.0 is required, while you have $pmmpThreadVersion.";
        }
    }

    if (!TerminalUtils::checkCommand("java")) {
        $messages[] = "[ERROR] Java is required. Please install at least Java 17.";
    }

    if (count($messages) > 0) {
        foreach ($messages as $message) {
            echo $message . PHP_EOL;
        }
        exit;
    }
}