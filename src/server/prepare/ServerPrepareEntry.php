<?php

namespace pocketcloud\cloud\server\prepare;

use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigType;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\util\FileUtils;
use const pocketcloud\GLOBAL_TEMPLATES_PATH;
use const pocketcloud\SERVER_GROUPS_PATH;

final class ServerPrepareEntry extends ThreadSafe {

    public function __construct(
        private readonly string $serverPath,
        private readonly string $templatePath,
        private readonly string $relativeLogFileLocation,
        private readonly ?string $group,
        private readonly bool $static,
        private readonly bool $alwaysCopyToStaticServers,
        private readonly string $templateType,
        private readonly ThreadSafeArray $propertiesData
    ) {}

    public function run(): void {
        $logFileLocation = $this->serverPath . $this->relativeLogFileLocation;
        if (file_exists($logFileLocation)) {
            if (!@file_exists($this->templatePath . "cloud_log_archive" . DIRECTORY_SEPARATOR)) @mkdir($this->templatePath . "cloud_log_archive", 0777, true);
            $ctime = filectime($logFileLocation) ?: time();
            FileUtils::copyFile($logFileLocation, $this->templatePath . "cloud_log_archive" . DIRECTORY_SEPARATOR . date("Y-m-d_H:i:s.v_e", $ctime) . "_" . basename($logFileLocation) . ".log");
            @unlink($logFileLocation);
        }

        if (file_exists($this->serverPath) && !$this->static) FileUtils::removeDirectory($this->serverPath);

        FileUtils::copyDirectory(GLOBAL_TEMPLATES_PATH . strtolower($this->templateType) . DIRECTORY_SEPARATOR, $this->serverPath);

        $copyFromSources = $this->alwaysCopyToStaticServers || !$this->static;
        if ($copyFromSources) {
            if ($this->group !== null) FileUtils::copyDirectory(SERVER_GROUPS_PATH . $this->group . DIRECTORY_SEPARATOR, $this->serverPath);
            FileUtils::copyDirectory($this->templatePath, $this->serverPath);
        }

        foreach ($this->propertiesData as $properties) {
            [$fileName, $replacements] = $properties;
            $filePath = $this->serverPath . $fileName;
            if (!$copyFromSources) FileUtils::copyFile(GLOBAL_TEMPLATES_PATH . strtolower($this->templateType) . DIRECTORY_SEPARATOR . $fileName, $filePath);
            $this->processAndReplacePlaceholders($filePath, iterator_to_array($replacements));
        }

        if (@file_exists($logFileLocation)) {
            @unlink($logFileLocation);
        }
    }

    private function processAndReplacePlaceholders(string $filePath, array $replacements): void {
        $config = new Config($filePath);
        $this->replacePlaceholders($config, $config->getAll(), $replacements);
        $config->save(function (string $filePath, array $content, ConfigType $type): bool {
            $rawContent = $type->encodeContent($content);
            return is_int(file_put_contents($filePath, str_replace("'", "", $rawContent)));
        });
    }

    private function replacePlaceholders(Config $config, array $data, array $replacements, ?string $initialKey = null): void {
        foreach ($data as $key => $item) {
            $fullKey = $initialKey !== null ? $initialKey . "." . $key : $key;
            if (is_array($item)) {
                $this->replacePlaceholders($config, $item, $replacements, $fullKey);
                continue;
            }

            if (is_string($item)) {
                foreach ($replacements as $replacementKey => $replacementValue) {
                    if (str_contains($item, $replacementKey)) {
                        $newValue = $replacementValue;
                        if ($item !== $replacementKey) $newValue = str_replace($replacementKey, $replacementValue, $item);
                        $config->set($fullKey, $newValue);
                    }
                }
            }
        }
    }

    public static function create(
        string $serverPath,
        string $templatePath,
        string $relativeLogFileLocation,
        ?string $group,
        bool $static,
        bool $alwaysCopyToStaticServers,
        string $templateType,
        array $propertiesData
    ): self {
        return new self($serverPath, $templatePath, $relativeLogFileLocation, $group, $static, $alwaysCopyToStaticServers, $templateType, ThreadSafeArray::fromArray($propertiesData));
    }

    public static function fromServer(CloudServer $server): self {
        return self::create(
            $server->getPath(),
            $server->getTemplate()->getPath(),
            $server->getTemplate()->getTemplateType()->getRelativeLogFileLocation(),
            ServerGroupManager::getInstance()->get($server->getTemplate())?->getName(),
            $server->getTemplate()->getSettings()->isStatic(),
            $server->getTemplate()->getSettings()->isAlwaysCopyToStaticServers(),
            $server->getTemplate()->getTemplateType()->getName(),
            self::createPropertiesData($server)
        );
    }

    public static function createPropertiesData(CloudServer $server): array {
        $properties = $server->getTemplate()->getTemplateType()->getAssignedProperties();
        $propertiesData = [];
        foreach ($properties as $property) {
            $propertiesData[] = [
                $property->getFileName(),
                $property->replacePlaceholders($server)
            ];
        }

        return $propertiesData;
    }
}