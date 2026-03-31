<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\command\parameter\def\StringEnumParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;

final class ListCommand extends Command {

    private const string DEFAULT_LISTING = "servers";

    public function __construct() {
        parent::__construct("list", "List all the plugins, servers, templates, server groups or players", ["ls"]);

        $this->addParameter(new StringEnumParameter("listing", ["plugins", "servers", "templates", "groups", "players"], false, true));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        $listing = $args["listing"] ?? self::DEFAULT_LISTING;
        switch (strtolower($listing)) {
            case "plugins": {
                CommandManager::getInstance()->handleInput($sender, "plugin", ["list"]);
                break;
            }
            case "servers": {
                CommandManager::getInstance()->handleInput($sender, "server", ["list"]);
                break;
            }
            case "templates": {
                CommandManager::getInstance()->handleInput($sender, "template", ["list"]);
                break;
            }
            case "players": {
                CommandManager::getInstance()->handleInput($sender, "player", ["list"]);
                break;
            }
            case "groups": {
                CommandManager::getInstance()->handleInput($sender, "group", ["list"]);
                break;
            }
        }

        return true;
    }
}