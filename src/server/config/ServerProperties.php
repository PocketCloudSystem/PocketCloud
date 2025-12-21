<?php

namespace pocketcloud\cloud\server\config;

use pocketcloud\cloud\template\TemplateType;

interface ServerProperties {

    public function modify(string $filePath, array $updatedContent): bool;

    public function renew(string $filePath): bool;

    public function needsRenewal(string $filePath): bool;

    public function getDefaultContent(): array;

    public function getFileName(): string;

    public function getTemplateType(): TemplateType;
}