<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\net\NetUtils;

final class ServerUtils {

    public const int DEFAULT_TIMEOUT = 20;
    public const int TIMEOUT_SERVER = 20;
    public const int TIMEOUT_PROXY = 25;

    private static array $ids = [];
    private static array $usedPorts = [];

    public static function addId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (!in_array($id, self::$ids[$template->getName()])) {
                self::$ids[$template->getName()][] = $id;
            }
        } else {
            self::$ids[$template->getName()] = [$id];
        }
    }

    public static function removeId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (in_array($id, self::$ids[$template->getName()])) {
                unset(self::$ids[$template->getName()][array_search($id, self::$ids[$template->getName()])]);
            }
        }
    }

    public static function getFreeId(Template $template): int {
        if (!isset(self::$ids[$template->getName()])) self::$ids[$template->getName()] = [];
        for ($i = 1; $i < ($template->getSettings()->getMaxServerCount() + 1); $i++) {
            if (!in_array($i, self::$ids[$template->getName()])) return $i;
        }
        return -1;
    }

    public static function addPort(int $port): void {
        if (!in_array($port, self::$usedPorts)) self::$usedPorts[] = $port;
    }

    public static function removePort(int $port): void {
        if (in_array($port, self::$usedPorts)) unset(self::$usedPorts[array_search($port, self::$usedPorts)]);
    }

    public static function getFreePort(TemplateType $type): int {
        [$start, $end, $randomPorts] = array_values($type->getServerPortRange());
        $currentPort = $start;
        while (true) {
            $port = !$randomPorts ? $currentPort++ : mt_rand($start, $end);
            $portV6 = $port + 1;
            if (
                !in_array($port, self::$usedPorts) && !in_array($portV6, self::$usedPorts) &&
                !NetUtils::isLocalUdpPortInUse($port) && !NetUtils::isLocalUdpPortInUse($portV6)
            ) break;
        }

        return $port;
    }
}