<?php

namespace pocketcloud\command\impl\web;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\language\Language;
use pocketcloud\util\Utils;
use pocketcloud\web\WebAccount;
use pocketcloud\web\WebAccountManager;
use pocketcloud\web\WebAccountRoles;

class WebAccountCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (!DefaultConfig::getInstance()->isWebEnabled()) {
            $sender->error(Language::current()->translate("command.web.disabled"));
            return true;
        }

        if (isset($args[0])) {
            $subCommand = strtolower(array_shift($args));
            if ($subCommand == "list") {
                $sender->info("Accounts §8(§e" . count($accounts = WebAccountManager::getInstance()->getAccounts()) . "§8)§r:");
                if (empty($accounts)) $sender->info("§cNo accounts available.");
                foreach ($accounts as $account) {
                    $sender->info(
                        "§e" . $account->getName() .
                        " §8- §risInitializePassword: §c" . ($account->isInitialPassword() ? Language::current()->translate("raw.yes") : "§a" . Language::current()->translate("raw.no")) .
                        " §8- §rRole: §e" . ($account->getRole()->roleName() == "default" ? "Default" : "§cAdmin")
                    );
                }
            } else if ($subCommand == "create") {
                if (count($args) == 0) {
                    $sender->error("§cwebaccount create <name> [role]");
                    return true;
                }

                $name = $args[0];
                if (WebAccountManager::getInstance()->checkAccount($name)) {
                    $sender->error(Language::current()->translate("webaccount.already.exists"));
                    return true;
                }

                $role = (isset($args[1]) ? (WebAccountRoles::from($args[1]) ?? WebAccountRoles::DEFAULT) : WebAccountRoles::DEFAULT);
                WebAccountManager::getInstance()->createAccount(new WebAccount($name, password_hash($initPassword = Utils::generateString(6), PASSWORD_BCRYPT), true, $role));
                $sender->info(Language::current()->translate("webaccount.created", $name, $initPassword));
            } else if ($subCommand == "remove") {
                if (count($args) == 0) {
                    $sender->error("§cwebaccount remove <name>");
                    return true;
                }

                $name = $args[0];
                if (($account = WebAccountManager::getInstance()->getAccount($name)) === null) {
                    $sender->error(Language::current()->translate("webaccount.doesnt.exists"));
                    return true;
                }

                WebAccountManager::getInstance()->removeAccount($account);
                $sender->info(Language::current()->translate("webaccount.removed", $name));
            } else if ($subCommand == "update") {
                if (count($args) < 3) {
                    $sender->error("§cwebaccount update <name> password OR role <value> ");
                    return true;
                }

                $name = $args[0];
                $action = strtolower($args[1]);
                $value = $args[2];

                if (($account = WebAccountManager::getInstance()->getAccount($name)) === null) {
                    $sender->error(Language::current()->translate("webaccount.doesnt.exists"));
                    return true;
                }

                if ($action == "password") {
                    WebAccountManager::getInstance()->updateAccount($account, password_hash($value, PASSWORD_BCRYPT), null);
                    $sender->info(Language::current()->translate("webaccount.password.updated"));
                } else if ($action == "role") {
                    if (($role = WebAccountRoles::from($value)) !== null) {
                        WebAccountManager::getInstance()->updateAccount($account, null, $role);
                        $sender->info(Language::current()->translate("webaccount.role.updated"));
                    } else $sender->info(Language::current()->translate("webaccount.role.not_found"));
                } else {
                    $sender->error("§cwebaccount update <name> role <value> ");
                }
            }
        } else return false;
        return true;
    }
}