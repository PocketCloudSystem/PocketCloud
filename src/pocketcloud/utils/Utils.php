<?php

namespace pocketcloud\utils;

use pocketcloud\scheduler\AsyncClosureTask;
use pocketcloud\scheduler\AsyncPool;

class Utils {

    private static mixed $lockFileHandle = null;
    private static string $startCommand = "";

    public static function checkRunning(?int &$pid = null): bool {
        if (!file_exists(STORAGE_PATH)) return false;
        $file = fopen(STORAGE_PATH . "cloud.lock", "a+b");
        if ($file === false) return false;
        if (!flock($file, LOCK_EX | LOCK_NB)) {
            flock($file, LOCK_SH);
            $processId = stream_get_contents($file);
            if(preg_match('/^\d+$/', $processId) === 1) {
                $pid = $processId;
            }
            return true;
        }
        return false;
    }

    public static function createLockFile() {
        $file = fopen(STORAGE_PATH . "cloud.lock", "a+b");
        if ($file === false) return;
        if (!flock($file, LOCK_EX | LOCK_NB)) flock($file, LOCK_SH);
        @ftruncate($file, 0);
        @fwrite($file, (string) getmypid());
        @fflush($file);
        @flock($file, LOCK_SH);
        self::$lockFileHandle = $file;
    }

    public static function deleteLockFile() {
        if (self::$lockFileHandle === null) return;
        @flock(self::$lockFileHandle, LOCK_UN);
        @fclose(self::$lockFileHandle);
        @unlink(STORAGE_PATH . "cloud.lock");
    }

    public static function executeWithStartCommand(string $path, string $name, string $softwareStartCommand) {
        if (self::$startCommand == "") return;
        passthru("cd " . $path . " && " . str_replace(["%name%", "%start_command%"], [$name, $softwareStartCommand], self::$startCommand));
    }

    public static function containKeys(array $array, ...$keys): bool {
        $result = true;
        foreach ($keys as $key) {
            if (!isset($array[$key])) $result = false;
        }
        return $result;
    }

    public static function createDefaultFiles() {
        if (!file_exists(STORAGE_PATH)) mkdir(STORAGE_PATH);
        if (!file_exists(CRASH_PATH)) mkdir(CRASH_PATH);
        if (!file_exists(PLUGINS_PATH)) mkdir(PLUGINS_PATH);
        if (!file_exists(SERVER_PLUGINS_PATH)) mkdir(SERVER_PLUGINS_PATH);
        if (!file_exists(PROXY_PLUGINS_PATH)) mkdir(PROXY_PLUGINS_PATH);
        if (!file_exists(CLOUD_PLUGINS_PATH)) mkdir(CLOUD_PLUGINS_PATH);
        if (!file_exists(SOFTWARE_PATH)) mkdir(SOFTWARE_PATH);
        if (!file_exists(IN_GAME_PATH)) mkdir(IN_GAME_PATH);
        if (!file_exists(LOG_PATH)) file_put_contents(LOG_PATH, "");
        if (!file_exists(TEMPLATES_PATH)) mkdir(TEMPLATES_PATH);
        if (!file_exists(TEMP_PATH)) mkdir(TEMP_PATH);
    }

    public static function deleteDir($dirPath) {
        $dirPath = rtrim($dirPath, DIRECTORY_SEPARATOR);
        if (is_dir($dirPath)) {
            foreach (array_diff(scandir($dirPath), [".", ".."]) as $object) {
                if (filetype($dirPath . DIRECTORY_SEPARATOR . $object) == "dir") {
                    self::deleteDir($dirPath . DIRECTORY_SEPARATOR . $object);
                } else {
                    try {
                        unlink($dirPath . DIRECTORY_SEPARATOR . $object);
                    } catch (\Throwable $exception) {
                        CloudLogger::get()->debug("Can't delete file: " . $dirPath . DIRECTORY_SEPARATOR . $object);
                    }
                }
            }

            try {
                rmdir($dirPath . DIRECTORY_SEPARATOR);
            } catch (\Throwable $exception) {
                CloudLogger::get()->debug("Can't delete dir: " . $dirPath . DIRECTORY_SEPARATOR);
            }
        }
    }

    public static function copyDir($src, $dst) {
        $src = rtrim($src, DIRECTORY_SEPARATOR);
        $dst = rtrim($dst, DIRECTORY_SEPARATOR);
        if (!file_exists($src)) @mkdir($src);
        if (!file_exists($dst)) @mkdir($dst);

        foreach (array_diff(scandir($src), [".", ".."]) as $file) {
            if (filetype($src . DIRECTORY_SEPARATOR . $file) == "dir") {
                self::copyDir($src . DIRECTORY_SEPARATOR . $file, $dst . DIRECTORY_SEPARATOR . $file);
            } else {
                try {
                    copy($src . DIRECTORY_SEPARATOR . $file, $dst . DIRECTORY_SEPARATOR . $file);
                } catch (\Throwable $exception) {
                    CloudLogger::get()->debug("Can't copy file from: " . $src . DIRECTORY_SEPARATOR . $file . " to " . $dst . DIRECTORY_SEPARATOR . $file);
                }
            }
        }
    }

    public static function copyFile($src, $dst) {
        $src = rtrim($src, DIRECTORY_SEPARATOR);
        $dst = rtrim($dst, DIRECTORY_SEPARATOR);
        if (!file_exists($src)) {
            try {
                if (is_file($src)) copy($src, $dst);
            } catch (\Throwable $exception) {
                CloudLogger::get()->debug("Can't copy file from: " . $src . " to " . $dst);
            }
        }
    }

    public static function cleanPath(string $path, bool $removePath = false): string {
        if ($removePath) return ($explode = explode(DIRECTORY_SEPARATOR, str_replace(["\\", "//", "/"], DIRECTORY_SEPARATOR, $path)))[count($explode) - 1];
        return str_replace(CLOUD_PATH, rtrim(str_replace("PocketCloud", "pcsrc", basename(CLOUD_PATH)), DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR, $path);
    }

    public static function check() {
        if (Utils::checkRunning($pid)) {
            CloudLogger::get()->error("Another instance of §bPocket§3Cloud §ris already running! (ProcessId: " . $pid . ")");
            exit(1);
        }

        if (PHP_OS_FAMILY == "Windows") {
            CloudLogger::get()->error("You can't use §bPocket§3Cloud §ron Windows!");
            exit(1);
        }

        if (!self::isBinaryExisting()) {
            CloudLogger::get()->error("Please install the following php binary in " . CLOUD_PATH . ":");
            CloudLogger::get()->error("§ehttps://jenkins.pmmp.io/job/PHP-8.0-Aggregate/lastSuccessfulBuild/artifact/PHP-8.0-Linux-x86_64.tar.gz");
            exit(1);
        }

        if (!self::isJavaInstalled()) {
            CloudLogger::get()->error("Please install Java 17!");
            CloudLogger::get()->error("Your operating system: §e" . php_uname());
            exit(1);
        }

        if (!self::detectStartMethod()) {
            CloudLogger::get()->error("Please install one of the following software:");
            CloudLogger::get()->error("tmux (apt-get install tmux)");
            CloudLogger::get()->error("Screen (apt-get install screen)");
            exit(1);
        }
    }

    public static function downloadFiles() {
        $downloadServerPlugin = false;
        $downloadProxyPlugin = false;

        if (!file_exists(SERVER_PLUGINS_PATH . "CloudBridge.phar")) $downloadServerPlugin = true;
        if (!file_exists(PROXY_PLUGINS_PATH . "CloudBridge.jar")) $downloadProxyPlugin = true;

        $github = false;
        $ch = curl_init();
        curl_setopt_array($ch, [
            CURLOPT_URL => "pocket-cloud.tk/",
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_FORBID_REUSE => 1,
            CURLOPT_FRESH_CONNECT => 1,
            CURLOPT_AUTOREFERER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_RETURNTRANSFER => true
        ]);
        curl_exec($ch);
        $errno = curl_errno($ch);
        if ($errno !== 0) $github = true;

        $serverPluginsPath = SERVER_PLUGINS_PATH;
        $proxyPluginsPath = PROXY_PLUGINS_PATH;

        if (!$github) {
            if ($downloadServerPlugin) AsyncPool::getInstance()->submitTask(AsyncClosureTask::fromClosure(fn() => self::download("https://pocket-cloud.tk/?file=CloudBridge.phar", $serverPluginsPath . "CloudBridge.phar")));
            if ($downloadProxyPlugin) AsyncPool::getInstance()->submitTask(AsyncClosureTask::fromClosure(fn() => self::download("https://pocket-cloud.tk/?file=CloudBridge.jar", $proxyPluginsPath . "CloudBridge.jar")));
        } else {
            if ($downloadServerPlugin) AsyncPool::getInstance()->submitTask(AsyncClosureTask::fromClosure(fn() => self::download("https://github.com/PocketCloudSystem/CloudBridge/releases/latest/download/CloudBridge.phar", $serverPluginsPath . "CloudBridge.phar")));
            if ($downloadProxyPlugin) AsyncPool::getInstance()->submitTask(AsyncClosureTask::fromClosure(fn() => self::download("https://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest/download/CloudBridge.jar", $proxyPluginsPath . "CloudBridge.jar")));
        }
    }

    public static function download(string $url, string $fileLocation): bool {
        $ch = curl_init();
        $fp = fopen($fileLocation, 'wb');

        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_HEADER => false,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_FILE => $fp,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_FAILONERROR => true
        ]);

        curl_exec($ch);
        return curl_errno($ch) == 0;
    }

    public static function detectStartMethod(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            if (self::isTmuxInstalled()) {
                self::$startCommand = "tmux new-session -d -s %name% bash -c '%start_command%'";
                return true;
            } else if (self::isScreenInstalled()) {
                self::$startCommand = "screen -dmS %name% %start_command%";
                return true;
            }
        }
        return false;
    }

    public static function isTmuxInstalled(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg("tmux")));
            return $output !== null && $output !== false;
        }
        return false;
    }

    public static function isScreenInstalled(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg("screen")));
            return $output !== null && $output !== false;
        }
        return false;
    }

    public static function isJavaInstalled(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg("java")));
            return $output !== null && $output !== false;
        }
        return false;
    }

    public static function isBinaryExisting(): bool {
        return self::getBinary() != "";
    }

    public static function getBinary(): string {
        $path = "";
        if (file_exists(CLOUD_PATH . "bin/")) {
            if (file_exists(CLOUD_PATH . "bin/php7/")) {
                if (file_exists(CLOUD_PATH . "bin/php7/bin/")) {
                    if (file_exists(CLOUD_PATH . "bin/php7/bin/php")) {
                        $path = CLOUD_PATH . "bin/php7/bin/php";
                    }
                }
            }
        }
        return $path;
    }

    public static function requireAll(string $dirPath) {
        foreach (array_diff(scandir($dirPath), [".", ".."]) as $file) {
            if (is_file($dirPath . "/" . $file)) {
                if (!class_exists($dirPath . "/" . $file)) include_once $dirPath . "/" . $file;
            } else if (is_dir($dirPath . "/" . $file)) {
                self::requireAll($dirPath . "/" . $file);
            }
        }
    }

    public static function kill(int $pid, bool $subprocesses = true): void {
        switch(PHP_OS_FAMILY) {
            case "Windows":
                exec("taskkill.exe /F " . ($subprocesses ? "/T " : "") . "/PID $pid > NUL 2> NUL");
                break;
            case "Linux":
            default:
                if($subprocesses) $pid = -$pid;

                if(function_exists("posix_kill")) {
                    posix_kill($pid, 9);
                } else {
                    exec("kill -9 $pid > /dev/null 2>&1");
                }
        }
    }

    public static function stringifyKeys(array $array): \Generator {
        foreach($array as $key => $value) {
            yield (string) $key => $value;
        }
    }

    public static function generateString(int $length = 5): string {
        $characters = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        $string = "";
        for ($i = 0; $i < $length; $i++) $string .= $characters[mt_rand(0, (strlen($characters) - 1))];
        return $string;
    }
}