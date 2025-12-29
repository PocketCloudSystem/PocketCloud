<?php

namespace pocketcloud\cloud\server\prepare;

use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FileUtils;
use const pocketcloud\GLOBAL_TEMPLATES_PATH;
use const pocketcloud\SERVER_GROUPS_PATH;

final class ServerPrepareEntry extends ThreadSafe {

    public function __construct(
        private readonly string $serverPath,
        private readonly string $templatePath,
        private readonly ?string $group,
        private readonly bool $static,
        private readonly bool $alwaysCopyToStaticServers,
        private readonly string $templateType,
        private readonly ThreadSafeArray $propertiesData
    ) {}

    public function run(): void {
        if (file_exists($this->serverPath) && !$this->static) FileUtils::removeDirectory($this->serverPath);
        $copyFromSources = $this->alwaysCopyToStaticServers || !$this->static;
        if ($copyFromSources) {
            FileUtils::copyDirectory(GLOBAL_TEMPLATES_PATH . strtolower($this->templateType) . DIRECTORY_SEPARATOR, $this->serverPath);
            if ($this->group !== null) FileUtils::copyDirectory(SERVER_GROUPS_PATH . $this->group . DIRECTORY_SEPARATOR, $this->serverPath);
            FileUtils::copyDirectory($this->templatePath, $this->serverPath);
        }

        foreach ($this->propertiesData as $properties) {
            [$fileName, $replacements] = $properties;
            if (!$copyFromSources) FileUtils::copyFile(GLOBAL_TEMPLATES_PATH . strtolower($this->templateType) . DIRECTORY_SEPARATOR . $fileName, $this->serverPath . $fileName);
            $replacements = iterator_to_array($replacements);
            $filePath = $this->serverPath . $fileName;
            FileUtils::filePutContents($filePath, str_replace(array_keys($replacements), array_values($replacements), FileUtils::fileGetContents($filePath)));
        }

        if (file_exists($this->serverPath . "server.log") || file_exists($this->serverPath . "logs/server.log")) {
            unlink(match ($this->templateType) {
                TemplateType::PROXY()->getName() => $this->serverPath . "logs/server.log",
                default => $this->serverPath . "server.log"
            });
        }
    }

    public static function create(
        string $serverPath,
        string $templatePath,
        ?string $group,
        bool $static,
        bool $alwaysCopyToStaticServers,
        string $templateType,
        array $propertiesData
    ): self {
        return new self($serverPath, $templatePath, $group, $static, $alwaysCopyToStaticServers, $templateType, ThreadSafeArray::fromArray($propertiesData));
    }

    public static function fromServer(CloudServer $server): self {
        return self::create(
            $server->getPath(),
            $server->getTemplate()->getPath(),
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