<?php

namespace pocketcloud\cloud\util\net;

use pocketcloud\cloud\terminal\log\CloudLogger;

final class NetUtils {

    public static function download(string $url, string $fileLocation): bool {
        CloudLogger::get()->debug("Downloading from " . $url . ", pasting into " . $fileLocation . "...", true);
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
}