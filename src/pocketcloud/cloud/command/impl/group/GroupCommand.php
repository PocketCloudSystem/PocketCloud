<?php

namespace pocketcloud\cloud\command\impl\group;

use pocketcloud\cloud\command\argument\def\MultipleTypesArgument;
use pocketcloud\cloud\command\argument\def\ServerGroupArgument;
use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\argument\def\TemplateArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\template\Template;

final class GroupCommand extends Command {

    public function __construct() {
        parent::__construct("group", "Manage server groups");

        $this->addParameter(new StringEnumArgument(
            "action",
            ["create", "remove", "addtemplate", "removetemplate", "list"],
            false,
            false
        ));

        $this->addParameter(new MultipleTypesArgument(
            "name",
            [
                new ServerGroupArgument("group", true),
                new StringArgument("name", false)
            ],
            true
        ));

        $this->addParameter(new TemplateArgument(
            "template",
            true,
            "The template was not found."
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $action = $args["action"];
        $name = $args["name"] ?? null;
        $template = $args["template"] ?? null;

        switch ($action) {
            case "create": {
                if (count($args) < 2) return false;
                if ($name instanceof ServerGroup) {
                    $sender->warn("A server group with that name already exists.");
                    return true;
                }

                $defTemplates = [];
                if ($template instanceof Template) $defTemplates = [$template->getName()];

                ServerGroupManager::getInstance()->create(new ServerGroup($name, $defTemplates));
                break;
            }
            case "remove": {
                if (count($args) < 2) return false;
                if (!$name instanceof ServerGroup) {
                    $sender->warn("A server group with that name does not exists.");
                    return true;
                }

                ServerGroupManager::getInstance()->remove($name);
                break;
            }
            case "addtemplate": {
                if (count($args) < 3) return false;
                if (!$name instanceof ServerGroup) {
                    $sender->warn("A server group with that name does not exists.");
                    return true;
                }

                if ($name->is($template)) {
                    $sender->warn("The template is already part of the server group §b" . $name->getName() . "§r.");
                    return true;
                }

                ServerGroupManager::getInstance()->addTemplate($name, $template);
                break;
            }
            case "removetemplate": {
                if (count($args) < 3) return false;
                if (!$name instanceof ServerGroup) {
                    $sender->warn("A server group with that name does not exists.");
                    return true;
                }

                if (!$name->is($template)) {
                    $sender->warn("The template is not part of the server group §b" . $name->getName() . "§r.");
                    return true;
                }

                ServerGroupManager::getInstance()->removeTemplate($name, $template);
                break;
            }
            case "list": {
                $sender->info("ServerGroups §8(§b" . count($groups = ServerGroupManager::getInstance()->getAll()) . "§8)§r:");
                if (empty($groups)) $sender->info("§c/");
                foreach ($groups as $group) {
                    $sender->info("§b" . $group->getName() .
                        " §8- §rTemplates: §b" . (empty($group->getTemplates()) ? "§c/" : implode("§8, §b", $group->getTemplates()))
                    );
                }
                break;
            }
            default: {
                return false;
            }
        }

        return true;
    }
}