<?php

namespace pocketcloud\cloud\update;

use Exception;
use JsonException;
use Phar;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\software\SoftwareManager;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\AsyncExecutor;
use pocketcloud\cloud\util\net\NetUtils;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\util\VersionInfo;

final class UpdateChecker {
    use SingletonTrait;

    private array $data = [];
    private bool $updating = false;

    public function __construct() {
        self::setInstance($this);
    }

    public function setUpdating(bool $updating): void {
        $this->updating = $updating;
    }

    public function check(): void {
        $this->checkCloud();
        $this->checkServerPlugin();
        $this->checkProxyPlugin();
        $this->checkServerSoftware();
        $this->checkProxySoftware();
    }

    private function checkCloud(): void {
        AsyncExecutor::execute(function(): false|string|null {
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
                CloudLogger::get()->exception($e);
                return false;
            }
        }, function(null|string|false $result): void {
            if ($result === false) {
                CloudLogger::get()->error("§cError occurred while checking for new updates!");
            } else if ($result === null) {
                CloudLogger::get()->error("§cThe API rate limit was exceeded for this IP address while checking for new updates!");
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
                    CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis outdated! Please install the newest version from §8'§bgithub.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                    CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $result);
                    CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
                } else {
                    if ($highVersion) {
                        CloudLogger::get()->warn("§cYour version of §bPocket§3Cloud §cis too HIGH! Please install the latest version from §8'§bgithub.com/PocketCloudSystem/PocketCloud/releases/latest§8'§c!");
                        CloudLogger::get()->warn("§cYour Version: §e" . VersionInfo::VERSION . " §8| §cLatest Version: §e" . $result);
                        CloudLogger::get()->warn("§cAlso make sure that the plugins are up to date!");
                    } else {
                        CloudLogger::get()->info("§rYour version of §bPocket§3Cloud §ris §aup to date§r!");
                    }
                }
            }
        });
    }

    private function checkServerPlugin(): void {
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
                $phar = new Phar(SERVER_PLUGINS_PATH . "CloudBridge.phar");
                if (isset($phar["plugin.yml"])) {
                    $yaml = yaml_parse($phar["plugin.yml"]->getContent());
                    if (isset($yaml["version"])) {
                        if ($yaml["version"] !== $data["tag_name"] && !VersionInfo::BETA) {
                            CloudLogger::get()->warn("§cYour version of the §bCloudBridge §cis outdated!");
                            if (MainConfig::getInstance()->isExecuteUpdates()) {
                                CloudLogger::get()->warn("§bDownloading §rthe newest version...");
                                $downloadNewest = true;
                            } else CloudLogger::get()->warn("§cPlease install the newest version from §8'§bhttps://github.com/PocketCloudSystem/CloudBridge/releases/latest§8'§c!");
                        }
                    }
                }
            }

            if ($downloadNewest) {
                @unlink(SERVER_PLUGINS_PATH . "CloudBridge.phar");
                Utils::downloadPlugins();
            }
        } catch (Exception $e) {
            CloudLogger::get()->exception($e);
        }
    }

    private function checkProxyPlugin(): void {
        try {
            $downloadNewest = false;
            $ch = curl_init("https://api.github.com/repos/PocketCloudSystem/CloudBridge-Proxy/releases/latest");
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
            if (is_array($data) && isset($data["assets"]) && is_array($data["assets"])) {
                foreach ($data["assets"] as $asset) {
                    if (isset($asset["name"]) && isset($asset["size"])) {
                        if ($asset["name"] == "CloudBridge.jar" && ($size = filesize(PROXY_PLUGINS_PATH . "CloudBridge.jar")) > 0) {
                            if ($asset["size"] !== $size) {
                                CloudLogger::get()->warn("§cYour version of the §bCloudBridge-Proxy §cis outdated!");
                                if (MainConfig::getInstance()->isExecuteUpdates()) {
                                    CloudLogger::get()->warn("§bDownloading §rthe newest version...");
                                    $downloadNewest = true;
                                } else CloudLogger::get()->warn("§cPlease install the newest version from §8'§bhttps://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest§8'§c!");
                            }
                        }
                    }
                }
            }

            if ($downloadNewest) {
                @unlink(PROXY_PLUGINS_PATH . "CloudBridge.jar");
                Utils::downloadPlugins();
            }
        } catch (Exception $e) {
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
            if (isset(($phar = new Phar(SOFTWARE_PATH . "PocketMine-MP.phar"))->getMetadata()["git"])) $pharGitCommit = $phar->getMetadata()["git"];

            if ($currentGitCommit !== $pharGitCommit) {
                CloudLogger::get()->warn("§cYour version of §bPocketMine-MP §cis outdated!");
                if (MainConfig::getInstance()->isExecuteUpdates()) {
                    CloudLogger::get()->warn("§bDownloading §rthe newest version...");
                    $downloadNewest = true;
                } else CloudLogger::get()->warn("§cPlease install the newest version from §8'§bhttps://github.com/pmmp/PocketMine-MP/releases/latest§8'§c!");
            }

            if ($downloadNewest) {
                SoftwareManager::getInstance()->removeAndDownload(SoftwareManager::getInstance()->get("PocketMine-MP"));
            }
        } catch (Exception $e) {
            CloudLogger::get()->exception($e);
        }
    }

    private function checkProxySoftware(): void {
        try {
            $software = SoftwareManager::getInstance()->get("WaterdogPE");
            $downloadNewest = false;
            $size = NetUtils::fileSize($software->getUrl());
            if ($size !== $software->getFileSize()) {
                CloudLogger::get()->warn("§cYour version of §bWaterdogPE §cis outdated!");
                if (MainConfig::getInstance()->isExecuteUpdates()) {
                    CloudLogger::get()->warn("§bDownloading §rthe newest version...");
                    $downloadNewest = true;
                } else CloudLogger::get()->warn("§cPlease install the newest version from §8'§bhttps://github.com/WaterdogPE/WaterdogPE/releases/latest§8'§c!");
            }

            if ($downloadNewest) {
                SoftwareManager::getInstance()->removeAndDownload($software);
            }
        } catch (Exception $e) {
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

    public function isUpdating(): bool {
        return $this->updating;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}