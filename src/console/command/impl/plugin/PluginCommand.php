<?php

namespace pocketcloud\cloud\console\command\impl\plugin;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\parameter\def\PluginParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginManager;

final class PluginCommand extends Command {

    public function __construct() {
        parent::__construct("plugin", "Manage the plugins", ["pl"]);

        $this->registerSubCommand(SubCommand::fromClosure("enable", $this->handleEnableSub(...))
            ->addParameter(new PluginParameter("plugin", false)));

        $this->registerSubCommand(SubCommand::fromClosure("disable", $this->handleDisableSub(...))
            ->addParameter(new PluginParameter("plugin", false)));

        $this->registerSubCommand(SubCommand::fromClosure("info", $this->handleInfoSub(...))
            ->addParameter(new PluginParameter("plugin", false)));

        $this->registerSubCommand(SubCommand::fromClosure("list", $this->handleListSub(...)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        return true;
    }

    public function handleEnableSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        /** @var CloudPlugin $plugin */
        $plugin = $args["plugin"];
        if ($plugin->isEnabled()) {
            $sender->error("The plugin is already enabled.");
            return true;
        }

        CloudPluginManager::getInstance()->enablePlugin($plugin);
        return true;
    }

    public function handleDisableSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        /** @var CloudPlugin $plugin */
        $plugin = $args["plugin"];
        if ($plugin->isDisabled()) {
            $sender->error("The plugin is already disabled.");
            return true;
        }

        CloudPluginManager::getInstance()->disablePlugin($plugin);
        return true;
    }

    public function handleInfoSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        /** @var CloudPlugin $plugin */
        $plugin = $args["plugin"];

        $sender->info("Plugin Info about §b{}§8:", $plugin->getDescription()->getFullName());
        $sender->info("Status§8: §b{}", $plugin->isEnabled() ? "§aEnabled" : "§cDisabled");
        $sender->info("Description§8: §b{}", $plugin->getDescription()->getDescription() ?? "None");
        $sender->info("Main Class§8: §b{} §8(§rSrcNamespacePrefix§8: §b{}§8)", $plugin->getDescription()->getMain(), $plugin->getDescription()->getSrcNamespacePrefix());
        $sender->info("Author(s)§8: §b{}", implode("§8, §b", $plugin->getDescription()->getAuthors()));
        $sender->info("Data Folder§8: §b{}", $plugin->getDataFolder());
        return true;
    }

    public function handleListSub(ICommandSender $sender, string $label, array $args, array $flags): bool {
        $sender->info("Plugins §8(§b{}§8):", count($plugins = CloudPluginManager::getInstance()->getAll()));
        if (empty($plugins)) $sender->info("§cNo plugins found.");
        $pluginMessageParts = [];
        foreach ($plugins as $plugin) {
            $pluginMessageParts[] = ($plugin->isEnabled() ? "§a" : "§c") .  $plugin->getDescription()->getName() . " v" . $plugin->getDescription()->getVersion();
        }

        if (!empty($pluginMessageParts)) $sender->info(implode("§8, §b", $pluginMessageParts));
        return true;
    }
}