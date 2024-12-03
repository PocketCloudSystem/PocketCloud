<?php

namespace pocketcloud\cloud\config\type;

final class PropertiesConfigType implements ConfigType {

    public function decodeContent(string $content): array {
        $result = [];
        if (preg_match_all('/^\s*([a-zA-Z0-9\-_\.]+)[ \t]*=([^\r\n]*)/um', $content, $matches) > 0) {
            foreach ($matches[1] as $i => $k) {
                $v = trim($matches[2][$i]);
                $v = match (strtolower($v)) {
                    "on", "true", "yes" => true,
                    "off", "false", "no" => false,
                    default => match ($v) {
                        (string)((int)$v) => (int)$v,
                        (string)((float)$v) => (float)$v,
                        default => $v,
                    },
                };
                $result[$k] = $v;
            }
        }

        return $result;
    }

    public function encodeContent(array $content): string {
        $result = "";
        foreach ($content as $k => $v) {
            if (is_bool($v)) $v = $v ? "on" : "off";
            $result .= $k . "=" . $v . "\r\n";
        }

        return $result;
    }
}