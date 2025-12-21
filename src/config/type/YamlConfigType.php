<?php

namespace pocketcloud\cloud\config\type;

final class YamlConfigType implements ConfigType {

    public function decodeContent(string $content): array {
        return yaml_parse($content);
    }

    public function encodeContent(array $content): string {
        return yaml_emit($content, YAML_UTF8_ENCODING);
    }
}