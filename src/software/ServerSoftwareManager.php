<?php

namespace pocketcloud\cloud\software;

use Phar;
use pocketcloud\cloud\config\impl\ServerSettingsConfig;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\net\NetUtils;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\SingletonTrait;
use ReflectionException;
use const pocketcloud\SOFTWARE_PATH;

final class ServerSoftwareManager implements Loadable {
    use SingletonTrait;

    /** @var array<ServerSoftware> */
    private array $software = [];

    public function __construct() {
        self::setInstance($this);
    }

    /**
     * @throws ReflectionException
     */
    public function load(): void {
        $this->register(new ServerSoftware(
            "PocketMine-MP", 
            ServerSettingsConfig::getInstance()->getStartCommand(ServerSettingsConfig::TYPE_SERVER),
            "https://github.com/pmmp/PocketMine-MP/releases/latest/download/PocketMine-MP.phar", 
            "PocketMine-MP.phar",
            ["pmmp"],
            function (ServerSoftware $software): Promise { // We want this to be sync to always be on the latest version
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
                if (isset(($phar = new Phar($software->getPath()))->getMetadata()["git"])) $pharGitCommit = $phar->getMetadata()["git"];
                return Promise::resolved($currentGitCommit !== $pharGitCommit);
            },
            function (ServerSoftware $software): Promise {
                return Promise::resolved(ServerSoftwareManager::getInstance()->removeAndDownload($software));
            }
        ));
        
        $this->register(new ServerSoftware(
            "WaterdogPE",
            ServerSettingsConfig::getInstance()->getStartCommand(ServerSettingsConfig::TYPE_PROXY),
            "https://github.com/WaterdogPE/WaterdogPE/releases/download/latest/Waterdog.jar", 
            "Waterdog.jar", 
            ["wdpe"],
            function (ServerSoftware $software): Promise {
                $size = NetUtils::fileSize($software->getUrl());
                return Promise::resolved($size !== $software->getFileSize());
            },
            function (ServerSoftware $software): Promise {
                return Promise::resolved(ServerSoftwareManager::getInstance()->removeAndDownload($software));
            }
        ));
    }

    public function downloadAll(): void {
        foreach ($this->software as $software) {
            if (!$this->check($software)) {
                if (!$this->download($software)) {
                    PocketCloud::getInstance()->shutdown();
                    break;
                }
            }
        }
    }

    public function download(ServerSoftware $software): bool {
        $temporaryLogger = CloudLogger::tmp();
        $temporaryLogger->info("Starting the download of software: {} ({})", $software->getName(), $software->getUrl());
        $result = NetUtils::download($software->getUrl(), SOFTWARE_PATH . $software->getFileName());
        if (!$result) {
            $temporaryLogger->warn("Failed to download software: {}", $software->getName());
            return false;
        }

        $temporaryLogger->success("Successfully downloaded software: {} ({}, {})", $software->getName(), FormatUtils::bytes($result), SOFTWARE_PATH . $software->getFileName());
        return true;
    }

    public function removeAndDownload(ServerSoftware $software): bool {
        if (file_exists(SOFTWARE_PATH . $software->getFileName())) @unlink(SOFTWARE_PATH . $software->getFileName());
        return $this->download($software);
    }

    public function check(ServerSoftware $software): bool {
        return file_exists(SOFTWARE_PATH . $software->getFileName());
    }

    public function register(ServerSoftware $software): bool {
        if (!isset($this->software[$software->getName()])) {
            $this->software[$software->getName()] = $software;
            return true;
        }
        return false;
    }

    public function unregister(ServerSoftware $software): bool {
        if (isset($this->software[$software->getName()])) {
            unset($this->software[$software->getName()]);
            return true;
        }
        return false;
    }

    public function get(string $name): ?ServerSoftware {
        return array_find($this->software, fn($software) => $software->getName() == $name ||
            in_array($name, $software->getAliases()));
    }

    public function getAll(): array {
        return $this->software;
    }
}