<?php

namespace pocketcloud\cloud\console\command\impl\template;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\ITabComplete;
use pocketcloud\cloud\console\command\parameter\def\StringEnumParameter;
use pocketcloud\cloud\console\command\parameter\def\StringParameter;
use pocketcloud\cloud\console\command\parameter\def\TemplateParameter;
use pocketcloud\cloud\console\command\parameter\def\TemplateTypeParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\setup\def\TemplateCreationSetup;
use pocketcloud\cloud\template\Template;
use pocketcloud\cloud\template\TemplateHelper;
use pocketcloud\cloud\template\TemplateManager;
use pocketcloud\cloud\template\TemplateSettings;
use pocketcloud\cloud\template\TemplateType;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\Utils;

final class TemplateCommand extends Command implements ITabComplete {

    public function __construct() {
        parent::__construct("template", "Manage your templates", ["temp"]);

        $this->registerSubCommand(SubCommand::fromClosure("create", $this->handleCreateSub(...), ["create"])
            ->addParameter(new StringParameter("name", true))
            ->addParameter(new StringParameter("templateType", true)));

        $this->registerSubCommand(SubCommand::fromClosure("edit", $this->handleEditSub(...), ["edit"])
            ->addParameter(new TemplateParameter("template", false))
            ->addParameter(new StringEnumParameter("key",  TemplateHelper::EDITABLE_KEYS, false, false))
            ->addParameter(new StringParameter("value", false)));

        $this->registerSubCommand(SubCommand::fromClosure("remove", $this->handleRemoveSub(...), ["remove"])
            ->addParameter(new TemplateParameter("template", false)));

        $this->registerSubCommand(SubCommand::fromClosure("list", $this->handleListSub(...))
            ->addParameter(new TemplateTypeParameter("type", true)));

        $this->registerSubCommand(SubCommand::fromClosure("info", $this->handleInfoSub(...))
            ->addParameter(new TemplateParameter("template", false)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }

    private function handleCreateSub(ICommandSender $sender, string $label, array $args): bool {
        if (count($args) == 0) {
            TemplateCreationSetup::new()->startSetup();
            return true;
        }

        $name = $args["name"] ?? null;
        $templateType = $args["templateType"] ?? null;
        if ($name === null || $templateType === null) return false;

        if (TemplateManager::getInstance()->check($name)) {
            $sender->error("Template already exists!");
        } else {
            TemplateManager::getInstance()->create(Template::create($name, TemplateSettings::default(), $templateType));
        }

        return true;
    }

    private function handleEditSub(ICommandSender $sender, string $label, array $args): bool {
        $template = $args["template"];
        $key = TemplateHelper::convert($args["key"]);
        $value = $args["value"];

        if (TemplateHelper::checkValue($value, $key, $expected, $realValue)) {
            TemplateManager::getInstance()->edit(
                $template,
                ($key == "lobby" ? $realValue : null),
                ($key == "maintenance" ? $realValue : null),
                ($key == "static" ? $realValue : null),
                ($key == "alwaysCopyToStaticServers" ? $realValue : null),
                ($key == "maxPlayerCount" ? $realValue : null),
                ($key == "minServerCount" ? $realValue : null),
                ($key == "maxServerCount" ? $realValue : null),
                ($key == "startNewPercentage" ? $realValue : null),
                ($key == "autoStart" ? $realValue : null),
            );
        } else $sender->error("Wrong value provided, expected an §b{} §rwhen editing §b{}§r.", $expected, $key);

        return true;
    }

    private function handleRemoveSub(ICommandSender $sender, string $label, array $args): bool {
        $template = $args["template"];
        $this->waitForConfirmation($sender, "§bAre you sure that you want to §cremove §bthe template §c" . $template->getName() . "§b?", ["yes", "y", "true", "t"])
            ->then(function (bool $confirmed) use ($sender, $template) {
                if ($confirmed) TemplateManager::getInstance()->remove($template);
            });
        return true;
    }

    public function handleListSub(ICommandSender $sender, string $label, array $args): bool {
        $type = $args["type"] ?? TemplateType::getAll();
        if (empty(TemplateManager::getInstance()->getAll(...$type))) $sender->info("§cNo templates found.");
        foreach (TemplateManager::getInstance()->getAll(...$type) as $template) {
            $sender->info(FormatUtils::implodeWithKeys(
                Utils::removeKeys($template->writeDetailed(), "statis", "alwaysCopyToStaticServers", "maxPlayerCount", "minServerCount", "maxServerCount", "startNewPercentage"),
                " §8| §r",
                "§8: §b",
                fn(string $key) => ucfirst($key),
                function (string $key, mixed $value) use($template): mixed {
                    if (in_array($key, ["lobby", "maintenance", "autoStart"])) {
                        return $value === true ? "§aYes" : "§cNo";
                    } else if ($key == "name") {
                        return $value . ($template->getParentServerGroup() !== null ? " §8(§b" . $template->getParentServerGroup()->getName() . "§8)" : "");
                    } else if ($key == "templateType") {
                        return strtoupper($value);
                    }

                    return $value;
                }
            ));
        }

        return true;
    }

    public function handleInfoSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var Template $template */
        $template = $args["template"];
        $sender->info("Information about §b{}§8:", $template->getName());
        foreach ($template->writeDetailed() as $key => $value) {
            $displayedKey = ucfirst($key);

            if (in_array($key, ["lobby", "maintenance", "static", "alwaysCopyToStaticServers", "autoStart"])) {
                $value = $value === true ? "§aYes" : "§cNo";
            } else if ($key == "name") {
                $value = $value . ($template->getParentServerGroup() !== null ? " §8(§b" . $template->getParentServerGroup()->getName() . "§8)" : "");
            } else if ($key == "startNewPercentage") {
                $value = $value . "%";
            } else if ($key == "templateType") {
                $value = strtoupper($value);
            }

            $sender->info($displayedKey . "§8: §b" . $value);
        }

        return true;
    }

    public function onTabComplete(array $args): array {
        if (count($args) == 4) {
            if ($args[0] == "edit") {
                $selectedKey = TemplateHelper::convert($args[2]);
                if (TemplateHelper::checkKey($selectedKey) && in_array($selectedKey, ["lobby", "maintenance", "autostart", "static", "alwayscopytostaticservers"])) {
                    return ["true", "false"];
                }
            }
        }
        return [];
    }
}