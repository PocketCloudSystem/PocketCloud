<?php

namespace pocketcloud\server\utils;

use pocketcloud\config\CloudConfig;
use pocketcloud\server\CloudServer;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\Config;
use pocketcloud\utils\Utils;

class PropertiesMaker {

    private const KEYS = [
        "SERVER" => [
            "language" => "eng",
            "motd" => "§b%name%",
            "server-port" => "%server_port%",
            "server-portv6" => "%server_portv6%",
            "enable-ipv6" => "on",
            "white-list" => "off",
            "max-players" => "%max_players%",
            "gamemode" => "0",
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
            "view-distance" => 8,
            "xbox-auth" => "off",
            "server-name" => "%name%",
            "template" => "%template%",
            "cloud-port" => "%port%",
            "cloud-path" => "%cloud_path%"
        ],
        "PROXY" => [
            "listener" => ["motd" => "%name%", "host" => "0.0.0.0:%server_port%", "max_players" => "%max_players%", "name" => "§bWaterdog§3PE"],
            "network_settings" => ["connection_throttle" => 10, "connection_throttle_time" => 1000, "enable_ipv6" => false, "max_user_mtu" => 1400, "login_throttle" => 2, "max_downstream_mtu" => 1400, "connection_timeout" => 15],
            "permissions" => ["r3pt1s" => ["waterdog.player.transfer", "waterdog.server.transfer", "waterdog.player.transfer.other", "waterdog.player.list", "waterdog.command.help", "waterdog.command.info", "waterdog.command.end"]],
            "permissions_default" => ["waterdog.command.help", "waterdog.command.info"],
            "enable_debug" => false,
            "upstream_encryption" => true,
            "online_mode" => true,
            "use_login_extras" => false,
            "replace_username_spaces" => false,
            "enable_query" => true,
            "prefer_fast_transfer" => true,
            "inject_proxy_commands" => true,
            "compression" => "zlib",
            "upstream_compression_level" => 6,
            "downstream_compression_level" => 2,
            "enable_edu_features" => false,
            "enable_packs" => false,
            "overwrite_client_packs" => false,
            "force_server_packs" => false,
            "pack_cache_size" => 16,
            "default_idle_threads" => -1,
            "enable-statistics" => true,
            "cloud-path" => "%cloud_path%",
            "cloud-port" => "%port%",
            "server-name" => "%name%",
            "template" => "%template%",
        ]
    ];

    public static function makeProperties(Template $template) {
        $fileName = ($template->getTemplateType() === TemplateType::SERVER() ? "server.properties" : "config.yml");
        $config = new Config($template->getPath() . $fileName, ($fileName == "server.properties" ? 0 : 2));
        foreach (self::KEYS[$template->getTemplateType()->getName()] as $key => $value) $config->set($key, $value);
        $config->save();
    }

    public static function copyProperties(CloudServer $server) {
        $fileName = ($server->getTemplate()->getTemplateType() === TemplateType::SERVER() ? "server.properties" : "config.yml");
        if (!file_exists($server->getTemplate()->getPath() . $fileName)) self::makeProperties($server->getTemplate());
        if (file_exists($server->getPath() . $fileName)) @unlink($server->getPath() . $fileName);
        Utils::copyFile($server->getTemplate()->getPath() . $fileName, $server->getPath() . $fileName);
        $content = file_get_contents($server->getPath() . $fileName);
        if ($content === false) return;
        file_put_contents($server->getPath() . $fileName, str_replace(
            ["%max_players%", "%server_port%", "%server_portv6%", "%name%", "%template%", "%port%", "%cloud_path%"],
            [$server->getCloudServerData()->getMaxPlayers(), $server->getCloudServerData()->getPort(), $server->getCloudServerData()->getPort()+1, $server->getName(), $server->getTemplate()->getName(), CloudConfig::getInstance()->getCloudPort(), CLOUD_PATH],
            $content
        ));
    }

    public static function getProperties(Template $template): Config {
        $fileName = ($template->getTemplateType() === TemplateType::SERVER() ? "server.properties" : "config.yml");
        if (!file_exists($template->getPath() . $fileName)) self::makeProperties($template);
        return new Config($template->getPath() . $fileName, ($fileName == "server.properties" ? 0 : 2));
    }
}