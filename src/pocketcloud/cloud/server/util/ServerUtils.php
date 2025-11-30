<?php

namespace pocketcloud\cloud\server\util;

use pocketcloud\cloud\config\Config;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\config\type\ConfigTypes;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FileUtils;

final class ServerUtils {

    public const int DEFAULT_TIMEOUT = 20;
    public const int TIMEOUT_SERVER = 20;
    public const int TIMEOUT_PROXY = 25;

    public const array PROPERTY_KEYS = [
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
            "encryption" => "%encryption%",
            "cloud-language" => "%language%",
            "cloud-path" => "%cloud_path%"
        ],
        "PROXY" => [
            "listener" => [
                "motd" => "%name%",
                "host" => "0.0.0.0:%server_port%",
                "max_players" => "%max_players%",
                "name" => "§bWaterdog§3PE",
                "forced_hosts" => "{}",
                "additional_ports" => "[]",
                "join_handler" => "DefaultJoinHandler",
                "reconnect_handler" => "DefaultReconnectHandler"
            ],
            "permissions" => [
                "r3pt1s" => ["waterdog.player.transfer", "waterdog.server.transfer", "waterdog.player.transfer.other", "waterdog.player.list", "waterdog.command.help", "waterdog.command.info", "waterdog.command.end"]
            ],
            "permissions_default" => ["waterdog.command.help", "waterdog.command.info"],
            "network_settings" => [
                "connection_throttle" => 10,
                "connection_throttle_time" => 1000,
                "enable_ipv6" => false,
                "max_user_mtu" => 1400,
                "login_throttle" => 2,
                "max_downstream_mtu" => 1400,
                "connection_timeout" => 15
            ],
            "enable_debug" => false,
            "upstream_encryption" => true,
            "online_mode" => true,
            "use_login_extras" => false,
            "replace_username_spaces" => false,
            "enable_query" => true,
            "compression" => "zlib",
            "prefer_fast_transfer" => true,
            "use_fast_codes" => true,
            "inject_proxy_commands" => true,
            "upstream_compression_level" => 6,
            "downstream_compression_level" => 2,
            "enable_edu_features" => true,
            "enable_packs" => true,
            "overwrite_client_packs" => false,
            "force_server_packs" => false,
            "pack_cache_size" => 16,
            "default_idle_threads" => -1,
            "enable-statistics" => false,
            "enable_error_reporting" => true,
            "cloud-port" => "%port%",
            "server-name" => "%name%",
            "template" => "%template%",
            "encryption" => "%encryption%",
            "cloud-language" => "%language%",
            "cloud-path" => "%cloud_path%"
        ]
    ];

    private static string $startCommand = "";
    private static array $ids = [];
    private static array $usedPorts = [];

    public static function addId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (!in_array($id, self::$ids[$template->getName()])) {
                self::$ids[$template->getName()][] = $id;
            }
        } else {
            self::$ids[$template->getName()] = [$id];
        }
    }

    public static function removeId(Template $template, int $id): void {
        if (isset(self::$ids[$template->getName()])) {
            if (in_array($id, self::$ids[$template->getName()])) {
                unset(self::$ids[$template->getName()][array_search($id, self::$ids[$template->getName()])]);
            }
        }
    }

    public static function getFreeId(Template $template): int {
        if (!isset(self::$ids[$template->getName()])) self::$ids[$template->getName()] = [];
        for ($i = 1; $i < ($template->getSettings()->getMaxServerCount() + 1); $i++) {
            if (!in_array($i, self::$ids[$template->getName()])) return $i;
        }
        return -1;
    }

    public static function addPort(int $port): void {
        if (!in_array($port, self::$usedPorts)) self::$usedPorts[] = $port;
    }

    public static function removePort(int $port): void {
        if (in_array($port, self::$usedPorts)) unset(self::$usedPorts[array_search($port, self::$usedPorts)]);
    }

    public static function getFreePort(): int {
        [$start, $end] = array_values(TemplateType::SERVER()->getServerPortRange());
        while (true) {
            $port = mt_rand($start ?? 40000, $end ?? 65535);
            $socket = socket_create(AF_INET, SOCK_DGRAM, SOL_UDP);
            $state = socket_connect($socket, "127.0.0.1", $port);

            if (!in_array([$port, $port+1], self::$usedPorts) && $state) {
                socket_close($socket);
                break;
            }
        }
        return $port;
    }

    public static function getFreeProxyPort(): int {
        [$start, $end] = array_values(TemplateType::PROXY()->getServerPortRange());
        for ($i = ($start ?? 19132); $i < ($end ?? 20000); $i++) {
            if (in_array($i, self::$usedPorts)) {
                continue;
            } else {
                return $i;
            }
        }
        return 0;
    }

    public static function makeProperties(Template $template): void {
        $fileName = ($template->getTemplateType() === TemplateType::SERVER() ? "server.properties" : "config.yml");
        if ($fileName == "server.properties") {
            $config = new Config($template->getPath() . $fileName, ConfigTypes::PROPERTIES());
            foreach (self::PROPERTY_KEYS[$template->getTemplateType()->getName()] as $key => $value) $config->set($key, $value);
            $config->save();
        } else {
            file_put_contents($template->getPath() . $fileName, str_replace("'", "", yaml_emit(self::PROPERTY_KEYS[$template->getTemplateType()->getName()], YAML_UTF8_ENCODING)));
        }
    }

    public static function copyProperties(CloudServer $server): void {
        $fileName = ($server->getTemplate()->getTemplateType() === TemplateType::SERVER() ? "server.properties" : "config.yml");
        if (!file_exists($server->getTemplate()->getPath() . $fileName)) self::makeProperties($server->getTemplate());
        if (file_exists($server->getPath() . $fileName)) unlink($server->getPath() . $fileName);
        FileUtils::copyFile($server->getTemplate()->getPath() . $fileName, $server->getPath() . $fileName);
        $content = file_get_contents($server->getPath() . $fileName);
        if ($content === false) return;
        file_put_contents($server->getPath() . $fileName, str_replace(
            ["%max_players%", "%server_port%", "%server_portv6%", "%name%", "%template%", "%port%", "%encryption%", "%language%", "%cloud_path%"],
            [
                $server->getCloudServerData()->getMaxPlayers(),
                $server->getCloudServerData()->getPort(),
                $server->getCloudServerData()->getPort()+1,
                $server->getName(),
                $server->getTemplate()->getName(),
                MainConfig::getInstance()->getNetworkPort(),
                ($server->getTemplate()->getTemplateType()->isServer() ? (MainConfig::getInstance()->isNetworkEncryptionEnabled() ? "on" : "off") : (MainConfig::getInstance()->isNetworkEncryptionEnabled() ? "true" : "false")),
                MainConfig::getInstance()->getLanguage(),
                CLOUD_PATH
            ],
            $content
        ));
    }

    public static function getProperties(Template $template): Config {
        $fileName = ($template->getTemplateType() === TemplateType::SERVER() ? "server.properties" : "config.yml");
        if (!file_exists($template->getPath() . $fileName)) self::makeProperties($template);
        return new Config($template->getPath() . $fileName, ($fileName == "server.properties" ? ConfigTypes::PROPERTIES() : ConfigTypes::YAML()));
    }

    public static function detectStartMethod(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            if (self::checkTmux()) {
                if (MainConfig::getInstance()->getStartMethod() == "screen") {
                    if (self::checkScreen()) {
                        self::$startCommand = "screen -dmS %name% %start_command%";
                        return true;
                    }
                }

                self::$startCommand = "tmux new-session -d -s %name% bash -c '%start_command%'";
                return true;
            } else if (self::checkScreen()) {
                if (MainConfig::getInstance()->getStartMethod() == "tmux") {
                    if (self::checkTmux()) {
                        self::$startCommand = "tmux new-session -d -s %name% bash -c '%start_command%'";
                        return true;
                    }
                }

                self::$startCommand = "screen -dmS %name% %start_command%";
                return true;
            }
        }
        return false;
    }

    public static function executeWithStartCommand(string $path, string $name, string $softwareStartCommand): void {
        if (self::$startCommand == "") return;
        passthru("cd " . $path . " && " . str_replace(["%name%", "%start_command%", "%SOFTWARE_PATH%", "%CLOUD_PATH%"], [$name, $softwareStartCommand, SOFTWARE_PATH, CLOUD_PATH], self::$startCommand));
    }

    public static function checkTmux(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg("tmux")));
            return $output !== null && $output !== false;
        }
        return false;
    }

    public static function checkScreen(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg("screen")));
            return $output !== null && $output !== false;
        }
        return false;
    }

    public static function checkJava(): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg("java")));
            return $output !== null && $output !== false;
        }
        return false;
    }

    public static function checkBinary(): bool {
        return self::getBinaryPath() != "";
    }

    public static function getBinaryPath(): string {
        $path = "";
        if (file_exists(CLOUD_PATH . "bin/php7/bin/php")) $path = CLOUD_PATH . "bin/php7/bin/php";
        return $path;
    }
}