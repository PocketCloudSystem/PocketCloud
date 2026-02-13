<?php

namespace pocketcloud\cloud\console\command\impl\player;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\parameter\def\PlayerParameter;
use pocketcloud\cloud\console\command\parameter\def\ServerParameter;
use pocketcloud\cloud\console\command\parameter\def\StringEnumParameter;
use pocketcloud\cloud\console\command\parameter\def\StringParameter;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\network\packet\data\TextType;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;
use pocketcloud\cloud\server\CloudServer;
use UnitEnum;

final class PlayerCommand extends Command {

    public function __construct() {
        parent::__construct("player", "Manage the players", ["ppl"]);

        $this->registerSubCommand(SubCommand::fromClosure("kick", $this->handleKickSub(...), ["kick"])
            ->addParameter(new PlayerParameter("player", false))
            ->addParameter(new StringParameter("reason", true, true)));

        $this->registerSubCommand(SubCommand::fromClosure("text", $this->handleTextSub(...))
            ->addParameter(new PlayerParameter("player", false))
            ->addParameter(new StringEnumParameter("type", array_map(fn(UnitEnum $enum) => strtolower($enum->name), TextType::cases()), false, false))
            ->addParameter(new StringParameter("text", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("transfer", $this->handleTransferSub(...), ["transfer"])
            ->addParameter(new PlayerParameter("player", false))
            ->addParameter(new ServerParameter("target", false)));

        $this->registerSubCommand(SubCommand::fromClosure("info", $this->handleinfoSub(...))
            ->addParameter(new PlayerParameter("player", false)));

        $this->registerSubCommand(SubCommand::fromClosure("list", $this->handleListSub(...)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }

    public function handleKickSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudPlayer $player */
        $player = $args["player"];
        $reason = $args["reason"] ?? "";
        $player->kick($reason);
        $sender->success("Kicked §b{} §rfrom the server.", $player->getName());
        return true;
    }

    public function handleTextSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudPlayer $player */
        $player = $args["player"];
        $type = TextType::fromName($args["type"]);
        $text = $args["text"];
        $player->send($text, $type);
        $sender->success("Sent a text from type §b{} §rto §b{}§r.", $type->getName(), $player->getName());
        return true;
    }

    public function handleTransferSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudPlayer $player */
        $player = $args["player"];
        /** @var CloudServer $server */
        $server = $args["target"];
        $player->transfer($server);
        $sender->success("Transferred §b{} §rto §b{}§r,", $player->getName(), $server->getName());
        return true;
    }

    public function handleInfoSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudPlayer $player */
        $player = $args["player"];
        $sender->info("Player Info about §b{}§8:", $player->getName());
        $sender->info("XboxUserId§8: §b{}", $player->getXboxUserId());
        $sender->info("UniqueId§8: §b{}", $player->getUniqueId());
        $sender->info("CurrentServer§8: §b{}", $player->getCurrentServerName() ?? "None");
        $sender->info("CurrentProxy§8: §b{}", $player->getCurrentProxyName() ?? "None");
        return true;
    }

    public function handleListSub(ICommandSender $sender, string $label, array $args): bool {
        $sender->info("Players §8(§b{}§8):", count($players = CloudPlayerManager::getInstance()->getAll()));
        if (empty($players)) {
            $sender->info("§cNo players online.");
        } else {
            foreach ($players as $player) {
                $sender->info("§b{} §8- §rCurrentServer: §b{} §8- §rCurrentProxy: §b{}", $player->getName(), $player->getCurrentServerName() ?? "None", $player->getCurrentProxyName() ?? "None");
            }
        }
        
        return true;
    }
}