<?php

namespace pocketcloud\cloud\server\config\def;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigTypeList;
use pocketcloud\cloud\server\config\ServerProperties;
use pocketcloud\cloud\template\TemplateType;

final class PocketMineServerProperties implements ServerProperties {

    public function modify(string $filePath, array $updatedContent): bool {
        $config = new Config($filePath, ConfigTypeList::PROPERTIES());
        foreach ($updatedContent as $name => $value) {
            $config->set($name, $value);
        }

        return $config->save();
    }

    public function renew(string $filePath): bool {
        $config = new Config($filePath, ConfigTypeList::PROPERTIES());
        foreach ($this->getDefaultContent() as $name => $value) {
            if (!$config->has($name)) $config->set($name, $value);
        }

        return $config->save();
    }

    public function needsRenewal(string $filePath): bool {
        if (file_exists($filePath)) {
            $config = new Config($filePath, ConfigTypeList::PROPERTIES());
            $keys = array_keys($this->getDefaultContent());
            $currentKeys = $config->getAll(true);
            return array_any($keys, fn(string $key) => !in_array($key, $currentKeys));
        }

        return true;
    }

    public function getDefaultContent(): array {
        return [
            "language" => "eng",
            "motd" => "§b%name%",
            "server-port" => "%server_port%",
            "server-portv6" => "%server_portv6%",
            "enable-ipv6" => "on",
            "white-list" => "off",
            "max-players" => "%max_players%",
            "gamemode" => "SURVIVAL",
            "force-gamemode" => "off",
            "hardcore" => "off",
            "pvp" => "on",
            "difficulty" => 2,
            "generator-settings" => "",
            "level-name" => "world",
            "level-seed" => "",
            "level-type" => "DEFAULT",
            "enable-query" => "on",
            "auto-save" => "off",
            "view-distance" => 16,
            "xbox-auth" => "off",
            "server-name" => "%name%",
            "template" => "%template%",
            "cloud-port" => "%port%",
            "encryption" => "%encryption%",
            "cloud-language" => "%language%",
            "cloud-path" => "%cloud_path%"
        ];
    }

    public function getFileName(): string {
        return "server.properties";
    }

    public function getTemplateType(): TemplateType {
        return TemplateType::SERVER();
    }
}