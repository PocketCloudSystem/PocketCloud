<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\flag\CommandFlag;
use pocketcloud\cloud\console\command\parameter\def\StringEnumParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\library\LibraryManager;
use pocketcloud\cloud\update\def\CloudPluginsUpdateChecker;
use pocketcloud\cloud\update\def\CloudUpdateChecker;
use pocketcloud\cloud\update\def\SoftwareUpdateChecker;
use pocketcloud\cloud\update\UpdateChecker;

final class UpdateCommand extends Command {

    public function __construct() {
        parent::__construct("update", "Checks for updates and installs them if possible.");
        $this->addFlag(CommandFlag::short("y"));
        $this->addParameter(new StringEnumParameter("which", ["cloud", "libs", "plugins", "server_software", "all"], false, true));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        $sure = $flags["y"] ?? false;
        $which = $args["which"] ?? "all";
        $updateFn = function (bool $v) use($which): void {
            if ($v) {
                if ($which == "all") {
                    LibraryManager::getInstance()->checkForUpdates(true);
                    UpdateChecker::getInstance()->checkForUpdates(true, [CloudPluginsUpdateChecker::class, CloudUpdateChecker::class]);
                } else {
                    if ($which == "libs") {
                        LibraryManager::getInstance()->checkForUpdates(true);
                    } else {
                        UpdateChecker::getInstance()->checkForUpdates(true, [match ($which) {
                            "server_software" => SoftwareUpdateChecker::class,
                            "plugins" => CloudPluginsUpdateChecker::class,
                            default => CloudUpdateChecker::class
                        }]);
                    }
                }
            }
        };

        if (!$sure) {
            $this->waitForConfirmation($sender, "§aAre you sure you want to check for updates and install them? You have to shutdown the cloud afterwards.", ["y", "yes"])
                ->then($updateFn);
        } else $updateFn(true);

        return true;
    }
}