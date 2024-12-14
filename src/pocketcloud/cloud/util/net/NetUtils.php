<?php

namespace pocketcloud\cloud\util\net;

final class NetUtils {

    public static function download(string $url, string $fileLocation): bool {
        $ch = curl_init();
        $fp = fopen($fileLocation, "wb");

        curl_setopt_array($ch, [
            CURLOPT_URL => $url,
            CURLOPT_HEADER => false,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_SSL_VERIFYPEER => false,
            CURLOPT_FAILONERROR => true
        ]);

        $data = curl_exec($ch);
        curl_close($ch);
        fwrite($fp, $data);
        fclose($fp);

        return curl_errno($ch) == 0;
    }
}