<?php

namespace pocketcloud\cloud\util\net;

use RuntimeException;

final class NetUtils {

    public static function isLocalUdpPortInUse(int $port, string $address = "0.0.0.0"): bool {
        $sock = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
        if ($sock === false) throw new RuntimeException("Unable to create socket");
        $ok = @socket_bind($sock, $address, $port);
        socket_close($sock);
        return $ok === false;
    }

    public static function download(string $url, string $fileLocation): bool {
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

        if ($success === false || $err !== 0 || $httpCode >= 400) {
            @unlink($tmpFile);
            return false;
        }

        rename($tmpFile, $fileLocation);
        return true;
    }


    public static function fileSize(string $url): ?int {
        $ch = curl_init($url);
        curl_setopt($ch, CURLOPT_NOBODY, true);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);

        curl_exec($ch);
        $size = curl_getinfo($ch, CURLINFO_CONTENT_LENGTH_DOWNLOAD);
        curl_close($ch);

        return $size !== -1 ? intval($size) : null;
    }
}