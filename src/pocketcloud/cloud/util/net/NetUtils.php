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

    public static function fileSize(string $url): ?int {
        $stream = fopen($url, "r", context: stream_context_create([
            "ssl" => [
                "verify_peer" => false,
                "verify_peer_name" => false
            ]
        ]));

        if ($stream) {
            return strlen(stream_get_contents($stream));
        }
        return null;
    }
}