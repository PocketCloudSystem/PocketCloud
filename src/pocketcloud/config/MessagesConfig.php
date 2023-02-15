<?php

namespace pocketcloud\config;

use pocketcloud\utils\Config;
use pocketcloud\utils\SingletonTrait;

class MessagesConfig {
    use SingletonTrait;

    private const MESSAGES = [
        "server-start" => "{PREFIX}§7The server §e%server% §7is §astarting§7...",
        "server-could-not-start" => "{PREFIX}§7The server §e%server% §ccouldn't §7be started§7!",
        "server-timed-out" => "{PREFIX}§7The server §e%server% §7is §ctimed out§7!",
        "server-stop" => "{PREFIX}§7The server §e%server% §7is §cstopping§7...",
        "server-crashed" => "{PREFIX}§7The server §e%server% §7was §ccrashed§7!",
        "proxy-stopped" => "§f§lProxy was stopped!",
        "cloud-command-description" => "Cloud Command",
        "cloud-notify-command-description" => "CloudNotify Command",
        "transfer-command-description" => "Transfer Command",
        "hub-command-description" => "Hub Command",
        "cloud-npc-command-description" => "CloudNPC Command",
        "no-permissions" => "{PREFIX}§7You don't have the permissions to use this command!",
        "request-timeout" => "§cRequest §e%0% §8(§e%1%§8) §ctimed out",
        "notifications-activated" => "{PREFIX}§7You are now getting notifications!",
        "notifications-deactivated" => "{PREFIX}§7You are now no longer getting notifications!",
        "cloud-help-usage" => "{PREFIX}§c/cloud start <template> [count: 1]\n{PREFIX}§c/cloud stop <template|server>\n{PREFIX}§c/cloud save\n{PREFIX}§c/cloud list [type (servers|templates|players): servers]",
        "cloud-list-help-usage" => "{PREFIX}§c/cloud list [type (servers|templates|players): servers]",
        "cloud-start-help-usage" => "{PREFIX}§c/cloud start <template> [count: 1]",
        "cloud-stop-help-usage" => "{PREFIX}§c/cloud stop <template|server>",
        "server-existence" => "{PREFIX}§cThe server doesn't exists!",
        "template-existence" => "{PREFIX}§cThe template doesn't exists!",
        "max-servers" => "{PREFIX}§7The maximum server amount for the template has been reached!",
        "server-saved" => "{PREFIX}§7The server was saved!",
        "template-maintenance" => "§cThis template is in maintenance!",
        "connect-to-server" => "{PREFIX}§7Connecting to server §e%0%§7...",
        "connect-to-server-target" => "{PREFIX}§e%0% §7is connecting to server §e%1%§7...",
        "already-connected" => "{PREFIX}§7You are already connected to the server §e%0%§7",
        "already-connected-target" => "{PREFIX}§e%0% is already connected to the server §e%1%§7",
        "cant-connect" => "{PREFIX}§7Can't connect to the server §e%0%§7!",
        "cant-connect-target" => "{PREFIX}§e%0% 7can't be connected to the server §e%1%§7!",
        "npc-name-tag" => "§7%0% playing.\n§8× §e§l%1%",
        "process-cancelled" => "{PREFIX}§7The process has been cancelled!",
        "select-npc" => "{PREFIX}§7Please select a npc to remove by hitting on them!",
        "already-npc" => "{PREFIX}§7There is already a cloud npc!",
        "npc-created" => "{PREFIX}§7The npc was successfully created!",
        "npc-removed" => "{PREFIX}§7The NPC has been successfully removed!",
        "no-server-found" => "{PREFIX}§7No server found!",
        "already-in-lobby" => "{PREFIX}§7You are already in a lobby!",
        "transfer-help-usage" => "{PREFIX}§c/transfer <server> [player]",
        "ui-npc-choose-server-title" => "§8» §eList §r§8| §e%0% §8«",
        "ui-npc-choose-server-text" => "§7There are currently §e%0% servers §7with the template §e%1% §7available.",
        "ui-npc-choose-server-button" => "§e%0%\n§8» §a%1%§8/§c%2%",
        "ui-npc-choose-server-no-server" => "§cNo server available!"
    ];

    private Config $config;
    private string $prefix;
    private string $serverStartMessage;
    private string $serverCouldNotStartMessage;
    private string $serverTimedOutMessage;
    private string $serverStopMessage;
    private string $serverCrashedMessage;
    private string $noPermissionsMessage;
    private string $notificationsActivated;
    private string $notificationsDeactivated;


    public function __construct() {
        self::setInstance($this);
        $this->config = new Config(IN_GAME_PATH . "messages.json", 1);

        if (!$this->config->exists("prefix")) $this->config->set("prefix", "§3§lPocket§bCloud §r§8» §7");
        foreach (self::MESSAGES as $key => $message) {
            if (!$this->config->exists($key)) $this->config->set($key, $message);
        }
        $this->config->save();

        $this->load();
    }

    private function load(): void {
        $this->prefix = $this->config->get("prefix");
        $this->serverStartMessage = $this->config->get("server-start");
        $this->serverCouldNotStartMessage = $this->config->get("server-could-not-start");
        $this->serverTimedOutMessage = $this->config->get("server-timed-out");
        $this->serverStopMessage = $this->config->get("server-stop");
        $this->serverCrashedMessage = $this->config->get("server-crashed");
        $this->noPermissionsMessage = $this->config->get("no-permissions");
        $this->notificationsActivated = $this->config->get("notifications-activated");
        $this->notificationsDeactivated = $this->config->get("notifications-deactivated");
    }

    public function reload(): void {
        $this->config->reload();
        $this->load();
    }

    public function getPrefix(): string {
        return $this->prefix;
    }

    public function getServerStartMessage(): string {
        return $this->serverStartMessage;
    }

    public function getServerCouldNotStartMessage(): string {
        return $this->serverCouldNotStartMessage;
    }

    public function getServerTimedOutMessage(): string {
        return $this->serverTimedOutMessage;
    }

    public function getServerStopMessage(): string {
        return $this->serverStopMessage;
    }

    public function getServerCrashedMessage(): string {
        return $this->serverCrashedMessage;
    }

    public function getNoPermissionsMessage(): string {
        return $this->noPermissionsMessage;
    }

    public function getNotificationsActivated(): string {
        return $this->notificationsActivated;
    }

    public function getNotificationsDeactivated(): string {
        return $this->notificationsDeactivated;
    }

    public function getConfig(): Config {
        return $this->config;
    }
}