<?php

namespace pocketcloud\cloud\server\config\def;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigTypeList;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\config\ServerProperties;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\Utils;

final class PocketMineConfig implements ServerProperties {

    public function modify(string $filePath, array $updatedContent): bool {
        $config = new Config($filePath, ConfigTypeList::YML());
        foreach ($updatedContent as $name => $value) {
            $config->set($name, $value);
        }

        return $config->save();
    }

    public function renew(string $filePath): bool {
        $config = new Config($filePath, ConfigTypeList::YML());
        $content = $config->getAll();
        Utils::fillMissingKeys($content, $this->getDefaultContent());
        $config->setAll($content);
        return $config->save();
    }

    public function needsRenewal(string $filePath): bool {
        if (file_exists($filePath)) {
            $config = new Config($filePath, ConfigTypeList::YML());
            $defaultContent = $this->getDefaultContent();
            $currentContent = $config->getAll();
            return !Utils::hasAllKeys($currentContent, $defaultContent);
        }

        return true;
    }

    public function replacePlaceholders(CloudServer $server): array {
        return [];
    }

    public function getDefaultContent(): array {
        return [
            "settings" => [
                "force-language" => false,
                "shutdown-message" => "Server closed",
                "query-plugins" => true,
                "enable-profiling" => false,
                "profile-report-trigger" => 20,
                "async-workers" => "auto",
                "enable-dev-builds" => false
            ],
            "memory" => [
                "global-limit" => 0,
                "main-limit" => 0,
                "main-hard-limit" => 1024,
                "async-worker-hard-limit" => 256,
                "check-rate" => 20,
                "continuous-trigger" => true,
                "continuous-trigger-rate" => 30,
                "garbage-collection" => [
                    "period" => 36000
                ],
                "memory-dump" => [
                    "dump-async-worker" => true
                ],
                "max-chunks" => [
                    "chunk-radius" => 4
                ],
            ],
            "network" => [
                "batch-threshold" => 256,
                "compression-level" => 6,
                "async-compression" => false,
                "async-compression-threshold" => 10000,
                "upnp-forwarding" => false,
                "max-mtu-size" => 1492,
                "enable-encryption" => true
            ],
            "debug" => [
                "level" => 1
            ],
            "player" => [
                "save-player-data" => true,
                "verify-xuid" => true
            ],
            "level-settings" => [
                "default-format" => "leveldb"
            ],
            "chunk-sending" => [
                "per-tick" => 4,
                "spawn-radius" => 4
            ],
            "chunk-ticking" => [
                "tick-radius" => 3,
                "blocks-per-subchunk-per-tick" => 3,
                "disable-block-ticking" => []
            ],
            "chunk-generation" => [
                "population-queue-size" => 32
            ],
            "ticks-per" => [
                "autosave" => 6000
            ],
            "auto-report" => [
                "enabled" => true,
                "send-code" => true,
                "send-settings" => true,
                "send-phpinfo" => false,
                "use-https" => true,
                "host" => "crash.pmmp.io"
            ],
            "anonymous-statistics" => [
                "enabled" => false,
                "host" => "stats.pocketmine.net"
            ],
            "auto-updater" => [
                "enabled" => true,
                "on-update" => [
                    "warn-console" => true
                ],
                "preferred-channel" => "stable",
                "suggest-channels" => true,
                "host" => "update.pmmp.io"
            ],
            "timings" => [
                "host" => "timings.pmmp.io"
            ],
            "console" => [
                "enable-input" => true,
                "title-tick" => true
            ],
            "aliases" => [],
            "worlds" => [],
            "plugins" => [
                "legacy-data-dir" => false
            ]
        ];
    }

    public function getFileName(): string {
        return "pocketmine.yml";
    }

    public function getTemplateType(): TemplateType {
        return TemplateType::SERVER();
    }
}