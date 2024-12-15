<?php

namespace pocketcloud\cloud\config\type;

interface ConfigType {

    public function decodeContent(string $content): array;

    public function encodeContent(array $content): string;
}