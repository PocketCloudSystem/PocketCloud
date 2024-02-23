<?php

namespace pocketcloud\update;

use pocketcloud\config\DefaultConfig;
use pocketcloud\language\Language;
use pocketcloud\software\SoftwareManager;
use pocketcloud\util\AsyncExecutor;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\SingletonTrait;
use pocketcloud\util\Utils;
use pocketcloud\util\VersionInfo;

class UpdateChecker {
    use SingletonTrait;

    private array $data = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function check(): void {
        $this->checkCloud();
        $this->checkPlugin();
        $this->checkServerSoftware();
    }

    private function checkCloud(): void {
        AsyncExecutor::execute(function(): false|string {
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
                    if (isset($data["tag_name"])) {
                        return $data["tag_name"];
                    } else {
                        return false;
                    }
                }
            } catch (\JsonException $e) {
                CloudLogger::get()->exception($e);
                return false;
            }
        }, function(null|string|false $result): void {
            if ($result === false || $result === null) {
                if (Language::current() === Language::ENGLISH()) CloudLogger::get()->error("§cError occurred while checking for new updates!");
                else CloudLogger::get()->error("§cEin Fehler ist während der Überprüfung von neuen Updates aufgetreten!");
            } else {
                $current = explode(".", UpdateChecker::getInstance()->getCurrentVersion());
                $latest = explode(".", $result);
                $outdated = false;
                $highVersion = false;

                $i = 0;
                foreach ($current as $number) {
                    if (intval($latest[$i]) > intval($number)) {
                        $outdated = true;
                        break;
                    } else if (intval($number) > intval($latest[$i])) {
                        $highVersion = !VersionInfo::BETA;
                        break;
                    }
                    $i++;
                }

                UpdateChecker::getInstance()->setData(["outdated" => $outdated, "newest_version" => $result]);

                if ($outdated) {
                    if (Language::current() === Language::GERMAN()) {
                        CloudLogger::get()->warn("§cDeine Version von §bPocket§3Cloud §cist nicht aktuell! Bitte installiere die neue Version von §8'§bhttps://github.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                        CloudLogger::get()->warn("§cDeine Version: §e" . VersionInfo::VERSION . " §8| §cNeuste Version: §e" . $result);
                        CloudLogger::get()->warn("§cStelle außerdem Sicher, dass deine Plugins aktuell sind!");
                    } else {
                        CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis outdated! Please install the newest version from §8'§bgithub.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                        CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $result);
                        CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
                    }
                } else {
                    if ($highVersion) {
                        if (Language::current() === Language::GERMAN()) {
                            CloudLogger::get()->warn("§cDeine Version von §bPocket§3Cloud §cist zu HOCH! Bitte installiere die neue Version von §8'§bhttps://github.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                            CloudLogger::get()->warn("§cDeine Version: §e" . VersionInfo::VERSION . " §8| §cNeuste Version: §e" . $result);
                            CloudLogger::get()->warn("§cStelle außerdem Sicher, dass deine Plugins aktuell sind!");
                        } else {
                            CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis too HIGH! Please install the latest version from §8'§bgithub.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                            CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $result);
                            CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
                        }
                    } else {
                        if (Language::current() === Language::GERMAN()) {
                            CloudLogger::get()->info("§aDeine Version von §bPocket§3Cloud §aist aktuell!");
                        } else {
                            CloudLogger::get()->info("§aYour version of §bPocket§3Cloud §ais up to date!");
                        }
                    }
                }
            }
        });
    }

    private function checkPlugin(): void {
        try {
            $downloadNewest = false;
            $ch = curl_init("https://api.github.com/repos/PocketCloudSystem/CloudBridge/releases/latest");
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
            if (is_array($data) && isset($data["tag_name"])) {
                $phar = new \Phar(SERVER_PLUGINS_PATH . "CloudBridge.phar");
                if (isset($phar["plugin.yml"])) {
                    $yaml = yaml_parse($phar["plugin.yml"]->getContent());
                    if (isset($yaml["version"])) {
                        if ($yaml["version"] !== $data["tag_name"]) {
                            if (Language::current() === Language::GERMAN()) {
                                CloudLogger::get()->warn("§cDeine Version von der §bCloudBridge §cist nicht aktuell!");
                                if (DefaultConfig::getInstance()->isExecuteUpdates()) {
                                    CloudLogger::get()->warn("§cNeuste Version wird heruntergeladen...");
                                    $downloadNewest = true;
                                } else CloudLogger::get()->warn("§cBitte installiere die neuste Version von §8'§bhttps://github.com/PocketCloudSystem/CloudBridge/releases/latest§8'§c!");
                            } else {
                                CloudLogger::get()->warn("§cYour version of the §bCloudBridge §cis outdated!");
                                if (DefaultConfig::getInstance()->isExecuteUpdates()) {
                                    CloudLogger::get()->warn("§cNewest version is downloading...");
                                    $downloadNewest = true;
                                } else CloudLogger::get()->warn("§cPlease install the newest version from §8'§bhttps://github.com/PocketCloudSystem/CloudBridge/releases/latest§8'§c!");
                            }
                        }
                    }
                }
            }

            if ($downloadNewest) {
                @unlink(SERVER_PLUGINS_PATH . "CloudBridge.phar");
                Utils::downloadFiles();
            }
        } catch (\Exception $e) {
            CloudLogger::get()->exception($e);
        }
    }

    private function checkServerSoftware(): void {
        try {
            $downloadNewest = false;
            $ch = curl_init("https://update.pmmp.io/api");
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
            $currentGitCommit = $data["git_commit"];
            $pharGitCommit = str_repeat("00", 20);
            if (isset(($phar = new \Phar(SOFTWARE_PATH . "PocketMine-MP.phar"))->getMetadata()["git"])) $pharGitCommit = $phar->getMetadata()["git"];

            if ($currentGitCommit !== $pharGitCommit) {
                if (Language::current() === Language::GERMAN()) {
                    CloudLogger::get()->warn("§cDeine Version von §bPocketMine-MP §cist nicht aktuell!");
                    if (DefaultConfig::getInstance()->isExecuteUpdates()) {
                        CloudLogger::get()->warn("§cNeuste Version wird heruntergeladen...");
                        $downloadNewest = true;
                    } else CloudLogger::get()->warn("§cBitte installiere die neuste Version von §8'§bhttps://github.com/pmmp/PocketMine-MP/releases/latest§8'§c!");
                } else {
                    CloudLogger::get()->warn("§cYour version of §bPocketMine-MP §cis outdated!");
                    if (DefaultConfig::getInstance()->isExecuteUpdates()) {
                        CloudLogger::get()->warn("§cNewest version is downloading...");
                        $downloadNewest = true;
                    } else CloudLogger::get()->warn("§cPlease install the newest version from §8'§bhttps://github.com/pmmp/PocketMine-MP/releases/latest§8'§c!");
                }
            }

            if ($downloadNewest) {
                SoftwareManager::getInstance()->downloadSoftware(SoftwareManager::getInstance()->getSoftwareByName("PocketMine-MP"));
            }
        } catch (\Exception $e) {
            CloudLogger::get()->exception($e);
        }
    }

    public function isOutdated(): ?bool {
        return $this->data["outdated"] ?? null;
    }

    public function isUpToDate(): bool {
        return !$this->isOutdated();
    }

    public function getNewestVersion(): ?string {
        return $this->data["newest_version"] ?? null;
    }

    public function getCurrentVersion(): string {
        return VersionInfo::VERSION;
    }

    public function setData(array $data): void {
        $this->data = $data;
    }

    public function getData(): array {
        return $this->data;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}