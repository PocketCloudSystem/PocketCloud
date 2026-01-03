<?php

namespace pocketcloud\cloud\server\config\def;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\config\type\ConfigTypeList;
use pocketcloud\cloud\network\client\ServerClientCache;
use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\config\ServerProperties;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\Utils;
use const pocketcloud\CLOUD_PATH;

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
        $content = $config->getAll();
        Utils::fillMissingKeys($content, $this->getDefaultContent());
        $config->setAll($content);
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

    public function replacePlaceholders(CloudServer $server): array {
        return [
            "%uuid%" => $server->getServerUuid(),
            "%name%" => $server->getName(),
            "%server_port%" => $server->getServerData()->getPort(),
            "%server_portv6%" => $server->getServerData()->getPort() + 1,
            "%max_players%" => $server->getTemplate()->getMaxPlayerCount(),
            "%template%" => $server->getTemplate()->getName(),
            "%address%" => Network::getInstance()->getAddress()->getAddress(),
            "%port%" => Network::getInstance()->getAddress()->getPort(),
            "%encryption%" => MainConfig::getInstance()->isNetworkEncryptionEnabled(),
            "%language%" => "eng",
            "%cloud_path%" => CLOUD_PATH,
            "%timeout%" => $server->getTemplate()->getTemplateType()->getServerTimeout(),
            "%auth_key%" => Network::getInstance()->getAuthenticationKey(),
            "%server_ip%" => count(ServerClientCache::getInstance()->getAll(...TemplateType::onlyProxy())) > 0 ? "127.0.0.1" : "0.0.0.0"
        ];
    }

    public function getDefaultContent(): array {
        return [
            "language" => "eng",
            "motd" => "§b%name%",
            "server-port" => "%server_port%",
            "server-portv6" => "%server_portv6%",
            "server-ip" => "%server_ip%",
            "server-ipv6" => "::1",
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
            "server-uuid" => "%uuid%",
            "server-name" => "%name%",
            "template" => "%template%",
            "cloud-address" => "%address%",
            "cloud-port" => "%port%",
            "network-encryption" => "%encryption%",
            "cloud-language" => "%language%",
            "cloud-path" => "%cloud_path%",
            "server-timeout" => "%timeout%",
            "auth-key" => "%auth_key%"
        ];
    }

    public function getFileName(): string {
        return "server.properties";
    }

    public function getTemplateType(): TemplateType {
        return TemplateType::SERVER();
    }
}