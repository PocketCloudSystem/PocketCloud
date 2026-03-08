<?php

namespace pocketcloud\cloud\template;

use Closure;
use Phar;
use pocketcloud\cloud\config\impl\ServerSettingsConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\server\config\ServerProperties;
use pocketcloud\cloud\server\config\ServerPropertiesGenerator;
use pocketcloud\cloud\software\ServerSoftware;
use pocketcloud\cloud\software\ServerSoftwareManager;
use pocketcloud\cloud\util\net\NetUtils;
use pocketcloud\cloud\util\PathUtils;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\RegistryTrait;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\util\VersionInfo;
use ReflectionException;
use Throwable;
use ZipArchive;
use const pocketcloud\GLOBAL_TEMPLATES_PATH;

/**
 * @method static TemplateType SERVER()
 * @method static TemplateType PROXY()
 */
final class TemplateType {
    use RegistryTrait;

    /**
     * @throws ReflectionException
     */
    protected static function init(): void {
        self::add(new TemplateType("server", ServerSoftwareManager::getInstance()->get("PocketMine-MP"), [
            "crashdumps", "log_archive", "players", "plugin_data", "plugins", "resource_packs",
            "virions", "worlds", "pocketmine.yml", "banned-ips.txt", "banned-players.txt", "ops.txt",
            "plugin_list.yml", "server.log", "white-list.txt"
        ], "save-all", "server.properties", "server.log", "plugins/CloudBridge.phar", false, function (TemplateType $type): bool {
            return NetUtils::download("https://github.com/PocketCloudSystem/CloudBridge/releases/latest/download/CloudBridge.phar", $type->getBridgeFileLocation());
        }, function (TemplateType $type): Promise {
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
                $phar = new Phar($type->getBridgeFileLocation());
                if (isset($phar["plugin.yml"])) {
                    $yaml = yaml_parse($phar["plugin.yml"]->getContent());
                    if (isset($yaml["version"])) {
                        if (version_compare($data["tag_name"], $yaml["version"], ">") && !VersionInfo::BETA) {
                            CloudLogger::get()->warn("§cYour version of the §bCloudBridge §cis outdated!");
                            return Promise::resolved([true]);
                        }
                    } else return Promise::resolved([true]);
                } else return Promise::resolved([true]);
            }

            return Promise::resolved(false);
        }, function (TemplateType $type): Promise {
            return Promise::resolved(NetUtils::download("https://github.com/PocketCloudSystem/CloudBridge/releases/latest/download/CloudBridge.phar", $type->getBridgeFileLocation()));
        }));

        self::add(new TemplateType("proxy", ServerSoftwareManager::getInstance()->get("WaterdogPE"), [
            "logs", "packs", "plugins", "lang.ini"
        ], null, "config.yml", "logs/server.log", "plugins/CloudBridge.jar", true, function (TemplateType $type): bool {
            return NetUtils::download("https://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest/download/CloudBridge.jar", $type->getBridgeFileLocation());
        }, function (TemplateType $type): Promise {
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
            if (is_array($data) && isset($data["tag_name"])) {
                try {
                    $zip = new ZipArchive();
                    if ($zip->open($type->getBridgeFileLocation())) {
                        $yaml = yaml_parse($zip->getFromName("plugin.yml"));
                        $zip->close();
                        if (version_compare($data["tag_name"], $yaml["version"], ">") && !VersionInfo::BETA) {
                            CloudLogger::get()->warn("§cYour version of the §bCloudBridge-Proxy §cis outdated!");
                            return Promise::resolved([true]);
                        }
                    }
                } catch (Throwable $exception) {
                    return Promise::rejected($exception->getMessage());
                }
            }

            return Promise::resolved(false);
        }, function (TemplateType $type): Promise {
            return Promise::resolved(NetUtils::download("https://github.com/PocketCloudSystem/CloudBridge-Proxy/releases/latest/download/CloudBridge.jar", $type->getBridgeFileLocation()));
        }));
    }

    public static function add(TemplateType $type): void {
        self::register(mb_strtoupper($type->getName()), $type);
    }

    public static function get(string $name): ?TemplateType {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    /** @return array<TemplateType> */
    public static function getAll(): array {
        self::check();
        return self::$members;
    }

    public static function onlyProxy(): array {
        self::check();
        return array_filter(self::$members, fn(TemplateType $type) => $type->isProxy());
    }

    public static function onlyNonProxy(): array {
        self::check();
        return array_filter(self::$members, fn(TemplateType $type) => !$type->isProxy());
    }

    /**
     * @throws ReflectionException
     */
    public function __construct(
        private readonly string $name,
        private readonly ServerSoftware $software,
        private readonly array $savableFiles,
        private readonly ?string $saveCommandLine,
        private readonly string $mainConfigurationFile,
        private readonly string $relativeLogFileLocation,
        private readonly string $bridgeFileLocation,
        private readonly bool $proxy,
        private readonly Closure $bridgePluginDownloadClosure,
        private readonly Closure $bridgePluginUpdateCheckClosure,
        private readonly Closure $bridgePluginUpdateClosure
    ) {
        Utils::validateCallbackSignature($this->bridgePluginDownloadClosure, [TemplateType::class], "bool");
        Utils::validateCallbackSignature($this->bridgePluginUpdateCheckClosure, [TemplateType::class], Promise::class);
        Utils::validateCallbackSignature($this->bridgePluginUpdateClosure, [TemplateType::class], Promise::class);
    }

    public function downloadBridge(): bool {
        return ($this->bridgePluginDownloadClosure)($this);
    }

    public function checkBridgeForUpdate(): Promise {
        if (!@file_exists($this->getBridgeFileLocation())) return Promise::resolved(true);
        return ($this->bridgePluginUpdateCheckClosure)($this);
    }

    public function updateBridge(): Promise {
        return ($this->bridgePluginDownloadClosure)($this);
    }

    public function checkBridge(): bool {
        return @file_exists($this->getBridgeFileLocation());
    }

    public function getName(): string {
        return $this->name;
    }

    public function getGlobalTemplatePath(): string {
        return PathUtils::join(GLOBAL_TEMPLATES_PATH, strtolower($this->name)) . "/";
    }

    public function getServerTimeout(): int {
        return ServerSettingsConfig::getInstance()->getServerTimeouts()[$this->name];
    }

    public function getServerPortRange(): array {
        return ServerSettingsConfig::getInstance()->getServerPortRanges()[$this->name];
    }

    public function getSoftware(): ServerSoftware {
        return $this->software;
    }

    public function getSavableFiles(): array {
        return $this->savableFiles;
    }

    public function getSaveCommandLine(): ?string {
        return $this->saveCommandLine;
    }

    public function getMainConfigurationFile(): string {
        return $this->mainConfigurationFile;
    }

    public function getRelativeLogFileLocation(): string {
        return $this->relativeLogFileLocation;
    }

    public function getRelativeBridgeFileLocation(): string {
        return $this->bridgeFileLocation;
    }

    public function getBridgeFileLocation(): string {
        return $this->getGlobalTemplatePath() . $this->bridgeFileLocation;
    }

    public function getBridgePluginDownloadClosure(): Closure {
        return $this->bridgePluginDownloadClosure;
    }

    public function getBridgePluginUpdateCheckClosure(): Closure {
        return $this->bridgePluginUpdateCheckClosure;
    }

    public function getBridgePluginUpdateClosure(): Closure {
        return $this->bridgePluginUpdateClosure;
    }

    public function isServer(): bool {
        return !$this->proxy;
    }

    public function isProxy(): bool {
        return $this->proxy;
    }

    /** @return array<ServerProperties> */
    public function getAssignedProperties(): array {
        return ServerPropertiesGenerator::getInstance()->getAll($this);
    }

    public function equals(TemplateType $type): bool {
        return $this->name === $type->getName();
    }

    public function __toString(): string {
        return $this->name;
    }
}