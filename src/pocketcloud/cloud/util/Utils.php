<?php

namespace pocketcloud\cloud\util;

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
            if(preg_match('/^\d+$/', $processId) === 1) {
                $pid = $processId;
            }
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

    public static function containKeys(array $array, ...$keys): bool {
        $result = true;
        foreach ($keys as $key) {
            if (!isset($array[$key])) $result = false;
        }
        return $result;
    }

    public static function cleanPath(string $path, bool $removePath = false): string {
        if ($removePath) return ($explode = explode(DIRECTORY_SEPARATOR, str_replace(["\\", "//", "/"], DIRECTORY_SEPARATOR, $path)))[count($explode) - 1];
        return str_replace(CLOUD_PATH, rtrim(str_replace("pocketcloud", "pcsrc", basename(CLOUD_PATH)), DIRECTORY_SEPARATOR) . DIRECTORY_SEPARATOR, $path);
    }
}