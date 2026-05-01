<?php

namespace pocketcloud\cloud\update\def;

use JsonException;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\update\IUpdateChecker;
use pocketcloud\cloud\update\UpdateChecker;
use pocketcloud\cloud\util\AsyncExecutor;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\VersionInfo;

final class CloudUpdateChecker implements IUpdateChecker {

    public function needsUpdate(bool $force = false): Promise {
        $promise = new Promise();
        AsyncExecutor::execute(function(): false|string|array|null {
            try {
                $ch = curl_init("https://api.github.com/repos/PocketCloudSystem/PocketCloud/releases/latest");
                curl_setopt_array($ch, [
                        CURLOPT_SSL_VERIFYPEER => false,
                        CURLOPT_SSL_VERIFYHOST => false,
                        CURLOPT_RETURNTRANSFER => true,
                        CURLOPT_HEADER => false,
                        CURLOPT_USERAGENT => "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)"
                    ]
                );

                $result = curl_exec($ch);
                $data = json_decode($result, true, flags: JSON_THROW_ON_ERROR);
                if ($data === false || $data === null) {
                    return false;
                } else {
                    if (isset($data["message"]) && str_contains($data["message"], "API rate limit")) return null;
                    return $data["tag_name"] ?? false;
                }
            } catch (JsonException $e) {
                return [false, $e->getMessage()];
            }
        }, function(null|string|array|false $result) use($promise): void {
            if (is_array($result)) {
                CloudLogger::get()->error("§cError occurred while checking for new cloud updates: §e{}", $result[1]);
                $promise->reject($result[1]);
            } else if ($result === false) {
                CloudLogger::get()->error("§cError occurred while checking for new cloud updates!");
                $promise->reject();
            } else if ($result === null) {
                CloudLogger::get()->warn("§cThe API rate limit was exceeded for this IP address while checking for new cloud updates!");
                $promise->resolve([false]);
            } else {
                $outdated = version_compare(VersionInfo::VERSION, $result, "<");
                $highVersion = version_compare(VersionInfo::VERSION, $result, ">");
                if ($outdated) {
                    CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis outdated! Please install the newest version from §8'§bhttps://github.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                    CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $result);
                    CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
                } else {
                    if ($highVersion) {
                        CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis too HIGH! Please install the latest version from §8'§bhttps://github.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                        CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $result);
                        CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
                    } else {
                        CloudLogger::get()->info("§rYour version of §bPocket§3Cloud §ris §aup to date§r!");
                    }
                }

                $promise->resolve([$outdated]);
            }
        });

        return $promise;
    }

    public function update(mixed $extraData): Promise {
        return Promise::resolved(true);
    }

    public function id(): string {
        return UpdateChecker::TYPE_CLOUD;
    }

    public function informManualUpdateRequired(mixed $extraData): void {}
}