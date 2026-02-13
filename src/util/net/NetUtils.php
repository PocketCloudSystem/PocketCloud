<?php

namespace pocketcloud\cloud\util\net;

use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\PocketCloud;
use RuntimeException;

final class NetUtils {

    public static function isLocalUdpPortInUse(int $port, string $address = "0.0.0.0"): bool {
        $sock = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
        if ($sock === false) throw new RuntimeException("Unable to create socket");
        $ok = @socket_bind($sock, $address, $port);
        socket_close($sock);
        return $ok === false;
    }

    public static function download(string $url, string $fileLocation): int|false {
        return ExceptionHandler::tryCatch(function (string $url, string $fileLocation): int|false {
            if (!@file_exists(dirname($fileLocation))) mkdir(dirname($fileLocation), 0777, true);
            $tmpFile = $fileLocation . ".tmp";
            $file = fopen($tmpFile, "wb");
            if (!$file) return false;

            $ch = curl_init($url);
            curl_setopt($ch, CURLOPT_FILE, $file);
            curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
            curl_setopt($ch, CURLOPT_FAILONERROR, true);
            curl_setopt($ch, CURLOPT_TIMEOUT, 60);
            curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 15);

            $success = curl_exec($ch);
            $err = curl_errno($ch);
            $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

            curl_close($ch);
            fclose($file);

            $size = filesize($tmpFile);

            if ($success === false || $err !== 0 || $httpCode >= 400) {
                @unlink($tmpFile);
                return false;
            }

            if (rename($tmpFile, $fileLocation)) return $size;
            return false;
        }, "Failed to download: " . $url, null, $url, $fileLocation) ?? false;
    }

    private static function tryHeadRequest(string $url): ?int {
        $ch = curl_init($url);

        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER => false,
            CURLOPT_NOBODY => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_USERAGENT => "Mozilla/5.0"
        ]);

        curl_exec($ch);

        if (curl_errno($ch)) {
            curl_close($ch);
            return null;
        }

        $size = curl_getinfo($ch, CURLINFO_CONTENT_LENGTH_DOWNLOAD);
        curl_close($ch);

        return ($size > 0) ? (int) $size : null;
    }

    private static function tryRangeRequest(string $url): ?int {
        $ch = curl_init($url);
        $totalSize = null;

        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_HEADER => false,
            CURLOPT_NOBODY => false,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_USERAGENT => "Mozilla/5.0",
            CURLOPT_RANGE => "0-0",
            CURLOPT_HEADERFUNCTION => function($curl, $header) use (&$totalSize) {
                if (preg_match("/Content-Range:\s*bytes\s+\d+-\d+\/(\d+)/i", $header, $m)) {
                    $totalSize = (int) $m[1];
                }
                return strlen($header);
            }
        ]);

        curl_exec($ch);

        if (curl_errno($ch)) {
            curl_close($ch);
            return null;
        }

        curl_close($ch);
        return $totalSize;
    }

    private static function tryFullDownload(string $url): ?int {
        $ch = curl_init($url);
        curl_setopt_array($ch, [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_USERAGENT => "Mozilla/5.0"
        ]);

        $content = curl_exec($ch);
        curl_close($ch);

        return $content !== false ? strlen($content) : null;
    }

    public static function fileSize(string $url): ?int {
        $size = self::tryHeadRequest($url);
        if ($size !== null) {
            return $size;
        }

        $size = self::tryRangeRequest($url);
        if ($size !== null) {
            return $size;
        }

        $size = self::tryFullDownload($url);
        if ($size !== null) {
            return $size;
        }

        return null;
    }
}