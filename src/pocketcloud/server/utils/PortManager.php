<?php

namespace pocketcloud\server\utils;

class PortManager {

    private static array $usedPorts = [];

    public static function addPort(int $port): void {
        if (!in_array($port, self::$usedPorts)) self::$usedPorts[] = $port;
    }

    public static function removePort(int $port): void {
        if (in_array($port, self::$usedPorts)) unset(self::$usedPorts[array_search($port, self::$usedPorts)]);
    }

    public static function getFreePort(): int {
        while (true) {
            $port = mt_rand(40000, 65535);
            $socket = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
            $state = socket_connect($socket, "127.0.0.1", $port);

            if (!in_array([$port, $port+1], self::$usedPorts) && $state) {
                socket_close($socket);
                break;
            }
        }
        return $port;
    }

    public static function getFreeProxyPort(): int {
        for ($i = 19132; $i < 20000; $i++) {
            if (in_array($i, self::$usedPorts)) {
                continue;
            } else {
                return $i;
            }
        }
        return 0;
    }
}