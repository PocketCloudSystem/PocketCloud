<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\net\NetUtils;

final class ServerUtils {

    public const int DEFAULT_TIMEOUT = 20;

    private static array $ids = [];
    private static array $usedPorts = [];

    public static function addId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (!isset(self::$ids[$template->getName()][$id])) {
                self::$ids[$template->getName()][$id] = $id;
            }
        } else {
            self::$ids[$template->getName()] = [$id => $id];
        }
    }

    public static function removeId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (isset(self::$ids[$template->getName()][$id])) {
                unset(self::$ids[$template->getName()][$id]);
            }
        }
    }

    public static function getFreeId(Template $template): int {
        if (!isset(self::$ids[$template->getName()])) self::$ids[$template->getName()] = [];
        for ($i = 1; $i < ($template->getSettings()->getMaxServerCount() + 1); $i++) {
            if (!isset(self::$ids[$template->getName()][$i])) return $i;
        }
        return -1;
    }

    public static function addPort(int $port): void {
        if (!isset(self::$usedPorts[$port])) self::$usedPorts[$port] = $port;
    }

    public static function removePort(int $port): void {
        if (isset(self::$usedPorts[$port])) unset(self::$usedPorts[$port]);
    }

    public static function getFreePort(TemplateType $type): int {
        [$start, $end, $randomPorts] = array_values($type->getServerPortRange());
        $currentPort = $start;
        $found = false;

        for ($tries = 0; $tries < 30; $tries++) {
            $port = !$randomPorts ? $currentPort++ : mt_rand($start, $end);
            $portV6 = $port + 1;
            if (
                !isset(self::$usedPorts[$port]) && !isset(self::$usedPorts[$portV6]) &&
                !NetUtils::isLocalUdpPortInUse($port) && !NetUtils::isLocalUdpPortInUse($portV6)
            ) {
                $found = true;
                break;
            }
        }

        return $found ? $port : 0;
    }
}