<?php

namespace pocketcloud\cloud\config\type;

use JsonException;

final class JsonConfigType implements ConfigType {

    /** @throws JsonException */
    public function decodeContent(string $content): array {
        return json_decode($content, true, flags: JSON_THROW_ON_ERROR);
    }

    /** @throws JsonException */
    public function encodeContent(array $content): string {
        return json_encode($content, flags: JSON_THROW_ON_ERROR | JSON_PRETTY_PRINT | JSON_BIGINT_AS_STRING | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    }
}