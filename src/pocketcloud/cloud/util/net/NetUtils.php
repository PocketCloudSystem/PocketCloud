<?php

namespace pocketcloud\cloud\util\net;

final class NetUtils {

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
}