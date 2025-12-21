<?php

namespace pocketcloud\cloud\server\config\def;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\type\ConfigTypeList;
use pocketcloud\cloud\server\config\ServerProperties;
use pocketcloud\cloud\template\TemplateType;

final class WaterdogConfig implements ServerProperties {

    public function modify(string $filePath, array $updatedContent): bool {
        $config = new Config($filePath, ConfigTypeList::YML());
        foreach ($updatedContent as $name => $value) {
            $config->set($name, $value);
        }

        return $config->save();
    }

    public function renew(string $filePath): bool {
        $config = new Config($filePath, ConfigTypeList::YML());
        foreach ($this->getDefaultContent() as $name => $value) {
            if (!$config->has($name)) $config->set($name, $value);
        }

        return $config->save();
    }

    public function needsRenewal(string $filePath): bool {
        if (file_exists($filePath)) {
            $config = new Config($filePath, ConfigTypeList::YML());
            $keys = array_keys($this->getDefaultContent());
            $currentKeys = $config->getAll(true);
            return array_any($keys, fn(string $key) => !in_array($key, $currentKeys));
        }

        return true;
    }

    public function getDefaultContent(): array {
        return [
            "listener" => [
                "motd" => "%name%",
                "name" => "§bWaterdog§3PE",
                "priorities" => [],
                "host" => "0.0.0.0:%server_port%",
                "max_players" => "%max_players%",
                "forced_hosts" => [],
                "additional_ports" => [],
                "join_handler" => "DefaultJoinHandler",
                "reconnect_handler" => "DefaultReconnectHandler"
            ],
            "servers" => [],
            "network_settings" => [
                "connection_throttle" => 10,
                "connection_throttle_time" => 1000,
                "enable_ipv6" => false,
                "max_user_mtu" => 1400,
                "enable_cookies" => true,
                "login_throttle" => 2,
                "max_downstream_mtu" => 1400,
                "connection_timeout" => 15
            ],
            "permissions" => [],
            "permissions_default" => [],
            "enable_debug" => false,
            "upstream_encryption" => true,
            "online_mode" => true,
            "use_login_extras" => false,
            "use_certificate_payload" => true,
            "replace_username_spaces" => false,
            "enable_query" => true,
            "prefer_fast_transfer" => true,
            "inject_proxy_commands" => true,
            "compression" => "zlib",
            "upstream_compression_level" => 6,
            "downstream_compression_level" => 2,
            "enable_edu_features" => true,
            "enable_packs" => true,
            "overwrite_client_packs" => false,
            "force_server_packs" => false,
            "pack_cache_size" => 16,
            "default_idle_threads" => -1,
            "enable_statistics" => true,
            "enable_error_reporting" => true,
            "cloud-port" => "%port%",
            "server-name" => "%name%",
            "template" => "%template%",
            "encryption" => "%encryption%",
            "cloud-language" => "%language%",
            "cloud-path" => "%cloud_path%"
        ];
    }

    public function getFileName(): string {
        return "config.yml";
    }

    public function getTemplateType(): TemplateType {
        return TemplateType::PROXY();
    }
}