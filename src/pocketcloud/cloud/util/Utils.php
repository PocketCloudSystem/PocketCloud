<?php

namespace pocketcloud\cloud\util;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\thread\ThreadManager;
use pocketcloud\cloud\util\net\NetUtils;
use Throwable;

final class Utils {

    private static mixed $lockFileHandle = null;

    public static function checkRunning(?int &$pid = null): bool {
        if (!file_exists(STORAGE_PATH)) return false;
        $file = fopen(STORAGE_PATH . "cloud.lock", "a+b");
        if ($file === false) return false;
        if (!flock($file, LOCK_EX | LOCK_NB)) {
            flock($file, LOCK_SH);
            $processId = stream_get_contents($file);
            if (preg_match('/^\d+$/', $processId) === 1) $pid = $processId;
            return true;
        }

        return false;
    }

    public static function createLockFile(): void {
        $file = fopen(STORAGE_PATH . "cloud.lock", "a+b");
        if ($file === false) return;
        if (!flock($file, LOCK_EX | LOCK_NB)) flock($file, LOCK_SH);
        ftruncate($file, 0);
        fwrite($file, (string) getmypid());
        fflush($file);
        flock($file, LOCK_SH);
        self::$lockFileHandle = $file;
    }

    public static function deleteLockFile(): void {
        try {
            if (self::$lockFileHandle === null) return;
            flock(self::$lockFileHandle, LOCK_UN);
            fclose(self::$lockFileHandle);
            unlink(STORAGE_PATH . "cloud.lock");
        } catch (Throwable) {}
    }

    public static function downloadPlugins(): void {
        $downloadServerPlugin = false;
        $downloadProxyPlugin = false;

        if (!file_exists(SERVER_PLUGINS_PATH . "CloudBridge.phar")) $downloadServerPlugin = true;
        if (!file_exists(PROXY_PLUGINS_PATH . "CloudBridge.jar")) $downloadProxyPlugin = true;

        $temporaryLogger = CloudLogger::temp(false);
        $serverPluginsPath = SERVER_PLUGINS_PATH;
        $proxyPluginsPath = PROXY_PLUGINS_PATH;

        if ($downloadServerPlugin) {
            $temporaryLogger->info("Starting the download for server plugin: %s", "CloudBridge.phar");
            NetUtils::download("https://github.com/PocketCloudSystem/CloudBridge/releases/latest/download/CloudBridge.phar", $serverPluginsPath . "CloudBridge.phar");
            $temporaryLogger->success("Successfully downloaded server plugin: %s (%s)", "CloudBridge.phar", $serverPluginsPath . "CloudBridge.phar");
        }

        if ($downloadProxyPlugin) {
            $temporaryLogger->info("Starting the download for proxy plugin: %s", "CloudBridge.phar");
            NetUtils::download("https://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest/download/CloudBridge.jar", $proxyPluginsPath . "CloudBridge.jar");
            $temporaryLogger->success("Successfully downloaded proxy plugin: %s (%s)", "CloudBridge.jar", $proxyPluginsPath . "CloudBridge.jar");
        }
    }

    public static function containKeys(array $array, ...$keys): bool {
        return array_all($keys, fn($key) => isset($array[$key]));
    }

    public static function cleanPath(string $path, bool $removePath = false): string {
        if ($removePath) return ($explode = explode(DIRECTORY_SEPARATOR, str_replace(["\\", "//", "/"], DIRECTORY_SEPARATOR, $path)))[count($explode) - 1];
        return str_replace(CLOUD_PATH, rtrim(str_replace("pocketcloud", "pcsrc", basename(CLOUD_PATH)), DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR, $path);
    }

    public static function readCloudPerformanceStatus(): array {
        $threadCount = count($threads = ThreadManager::getInstance()->getAll());
        $memoryLimit = MainConfig::getInstance()->getMemoryLimit();
        [$mainMemory, $mainMemoryPeak] = [memory_get_usage(), memory_get_peak_usage()];
        [$mainMemorySys, $mainMemorySysPeak] = [memory_get_usage(true), memory_get_peak_usage(true)];
        [$serverCount, $playerCount] = [count(CloudServerManager::getInstance()->getAll()), count(CloudPlayerManager::getInstance()->getAll())];
        return [$threadCount, $threads, $mainMemory, $mainMemoryPeak, $mainMemorySys, $mainMemorySysPeak, $memoryLimit, $serverCount, $playerCount];
    }

    public static function generateString(int $length = 5): string {
        $characters = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        $string = "";
        for ($i = 0; $i < $length; $i++) $string .= $characters[mt_rand(0, (strlen($characters) - 1))];
        return $string;
    }
}