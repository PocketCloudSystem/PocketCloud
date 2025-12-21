<?php

namespace pocketcloud\cloud\util;

final class NetUtils {

    public static function download(string $url, string $fileLocation): bool {
        $ch = curl_init($url);
        $file = fopen($fileLocation, 'wb');
        if (!$file) return false;

        curl_setopt($ch, CURLOPT_FILE, $file);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, true);
        curl_setopt($ch, CURLOPT_FAILONERROR, true);

        $result = curl_exec($ch);
        $err = curl_errno($ch);

        curl_close($ch);
        fclose($file);

        return $result !== false && $err === 0;
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