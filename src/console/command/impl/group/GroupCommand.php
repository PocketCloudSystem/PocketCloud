<?php

namespace pocketcloud\cloud\console\command\impl\group;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\parameter\def\ServerGroupParameter;
use pocketcloud\cloud\console\command\parameter\def\StringParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;
use pocketcloud\cloud\template\TemplateManager;

final class GroupCommand extends Command {

    public function __construct() {
        parent::__construct("group", "Manage server groups", ["g", "sg", "servergroup"]);

        $this->registerSubCommand(SubCommand::fromClosure("create", $this->handleCreateSub(...))
            ->addParameter(new StringParameter("name", false))
            ->addParameter(new StringParameter("templates", true, true)));

        $this->registerSubCommand(SubCommand::fromClosure("remove", $this->handleRemoveSub(...))
            ->addParameter(new ServerGroupParameter("group", false)));

        $this->registerSubCommand(SubCommand::fromClosure("addt", $this->handleAddTemplateSub(...))
            ->addParameter(new ServerGroupParameter("group", false))
            ->addParameter(new StringParameter("templates", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("removet", $this->handleRemoveTemplateSub(...))
            ->addParameter(new ServerGroupParameter("group", false))
            ->addParameter(new StringParameter("templates", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("list", $this->handleListSub(...)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }

    private function handleCreateSub(ICommandSender $sender, string $label, array $args): bool {
        $name = $args["name"];
        if (ServerGroupManager::getInstance()->check($name)) {
            $sender->warn("A server group with that name already exists.");
            return true;
        }

        $templates = $args["templates"] ?? [];
        if (is_string($templates)) {
            $templates = explode(" ", $templates);
            foreach ($templates as $i => $template) {
                if (!TemplateManager::getInstance()->check($template)) unset($templates[$i]);
            }
        }

        ServerGroupManager::getInstance()->create(new ServerGroup($name, array_values($templates)));
        return true;
    }

    private function handleRemoveSub(ICommandSender $sender, string $label, array $args): bool {
        ServerGroupManager::getInstance()->remove($args["group"]);
        return true;
    }

    private function handleAddTemplateSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var ServerGroup $group */
        $group = $args["group"];
        $templates = explode(" ", $args["templates"]);
        foreach ($templates as $template) {
            if (!TemplateManager::getInstance()->check($template)) {
                $sender->warn("A template with the name §b{} §rdoesn't exist.", $template);
                continue;
            }

            ServerGroupManager::getInstance()->addTemplate($group, TemplateManager::getInstance()->get($template));
        }

        return true;
    }

    private function handleRemoveTemplateSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var ServerGroup $group */
        $group = $args["group"];
        $templates = explode(" ", $args["templates"]);
        foreach ($templates as $template) {
            if (!TemplateManager::getInstance()->check($template)) {
                $sender->warn("A template with the name §b{} §rdoesn't exist.", $template);
                continue;
            }

            ServerGroupManager::getInstance()->removeTemplate($group, TemplateManager::getInstance()->get($template));
        }

        return true;
    }

    private function handleListSub(ICommandSender $sender, string $label, array $args): bool {
        $sender->info("ServerGroups §8(§b" . count($groups = ServerGroupManager::getInstance()->getAll()) . "§8)§r:");
        if (empty($groups)) $sender->info("§cNo server groups found.");
        foreach ($groups as $group) {
            $sender->info("§b{} §8| §rPlayerCount: §b{} §8| §r§rTemplates: §b{}", $group->getName(), $group->getPlayerCount(), empty($group->getTemplates()) ? "§cNone" : implode("§8, §b", $group->getTemplates()));
        }
        return true;
    }
}