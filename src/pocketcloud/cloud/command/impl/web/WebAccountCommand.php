<?php

namespace pocketcloud\cloud\command\impl\web;

use pocketcloud\cloud\command\argument\def\MultipleTypesArgument;
use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\argument\def\WebAccountArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\setup\impl\WebAccountSetup;
use pocketcloud\cloud\util\Utils;
use pocketcloud\cloud\web\WebAccount;
use pocketcloud\cloud\web\WebAccountManager;
use pocketcloud\cloud\web\WebAccountRoles;

final class WebAccountCommand extends Command {

    public function __construct() {
        parent::__construct("webaccount", "Manage web accounts");

        $this->addParameter(new StringEnumArgument(
            "action",
            ["list", "create", "remove", "update"],
            false,
            false,
            "Please provide a supported action."
        ));

        $this->addParameter(new MultipleTypesArgument(
            "name",
            [
                new WebAccountArgument("account", true),
                new StringArgument("name", true)
            ],
            true
        ));

        $this->addParameter(new StringEnumArgument(
            "update_action",
            ["password", "role"],
            false,
            true,
            "Please provide a supported action to update the web account."
        ));

        $this->addParameter(new StringArgument(
            "value",
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        if (!MainConfig::getInstance()->isWebEnabled()) {
            $sender->error("The web command is currently §cdisabled§r.");
            return true;
        }

        $subCommand = $args["action"];
        if ($subCommand == "list") {
            $sender->info("Accounts §8(§e" . count($accounts = WebAccountManager::getInstance()->getAll()) . "§8)§r:");
            if (empty($accounts)) $sender->info("§cNo accounts available.");
            foreach ($accounts as $account) {
                $sender->info(
                    "§e" . $account->getName() .
                    " §8- §risInitialPassword: §c" . ($account->isInitialPassword() ? "Yes" : "§aNo") .
                    " §8- §rRole: §b" . ($account->getRole()->roleName() == "default" ? "Default" : "§cAdmin")
                );
            }
        } else if ($subCommand == "create") {
            if (count($args) < 2) {
                new WebAccountSetup()->startSetup();
                return true;
            }

            $name = $args["name"];
            if (WebAccountManager::getInstance()->check($name)) {
                $sender->error("Failed to create the web account, an account with that name already exists.");
                return true;
            }

            WebAccountManager::getInstance()->create(new WebAccount($name, password_hash($initPassword = Utils::generateString(6), PASSWORD_BCRYPT), true, WebAccountRoles::DEFAULT));
            $sender->success("Successfully §acreated §rthe web account §b" . $name . " §rwith the role §bdefault§r. §8(§rInitial Password: §b" . $initPassword . "§8)");
        } else if ($subCommand == "remove") {
            if (count($args) < 2) return false;

            $account = $args["name"];
            if (!$account instanceof WebAccount) {
                $sender->error("The web account does not exist!");
                return true;
            }

            WebAccountManager::getInstance()->remove($account);
            $sender->success("Successfully §cremoved §rthe web account §b" . $account->getName() . "§r.");
        } else if ($subCommand == "update") {
            if (count($args) < 4) {
                return false;
            }

            $account = $args["name"];
            if (!$account instanceof WebAccount) {
                $sender->error("The web account does not exist!");
                return true;
            }

            $action = $args["update_action"];
            $value = $args["value"];

            if ($action == "password") {
                WebAccountManager::getInstance()->update($account, password_hash($value, PASSWORD_BCRYPT), null);
                $sender->success("Successfully §aupdated §rthe §bweb account§r.");
            } else if ($action == "role") {
                if (($role = WebAccountRoles::get($value)) !== null) {
                    WebAccountManager::getInstance()->update($account, null, $role);
                    $sender->success("Successfully §aupdated §rthe role of the §bweb account§r.");
                } else $sender->warn("The web role does not exist!");
            } else {
                return false;
            }
        }
        return true;
    }
}