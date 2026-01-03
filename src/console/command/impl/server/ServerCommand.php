<?php

namespace pocketcloud\cloud\console\command\impl\server;

use pocketcloud\cloud\console\command\parameter\def\BoolParameter;
use pocketcloud\cloud\console\command\parameter\def\IntegerParameter;
use pocketcloud\cloud\console\command\parameter\def\MultipleTypesParameter;
use pocketcloud\cloud\console\command\parameter\def\ServerParameter;
use pocketcloud\cloud\console\command\parameter\def\ServerGroupParameter;
use pocketcloud\cloud\console\command\parameter\def\StringParameter;
use pocketcloud\cloud\console\command\parameter\def\StringEnumParameter;
use pocketcloud\cloud\console\command\parameter\def\TemplateParameter;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\FormatUtils;

final class ServerCommand extends Command {

    public function __construct() {
        parent::__construct("server", "Manage the cloud's servers", ["srv"]);

        $this->registerSubCommand(SubCommand::fromClosure("start", function (ICommandSender $sender, string $label, array $args): bool {
            $template = $args["template"];
            $amount = $args["amount"] ?? 1;
            CloudServerManager::getInstance()->start($template, $amount);
            return true;
        })->addParameter(new TemplateParameter("template", false))->addParameter(new IntegerParameter("amount", true, function (int $number): int {
            if ($number < 0 || $number > 20) return 1;
            return $number;
        })));

        $this->registerSubCommand(SubCommand::fromClosure("stop", function (ICommandSender $sender, string $label, array $args): bool {
            $server = $args["server"];
            $forcefully = $args["forcefully"] ?? false;

            if (is_string($server) && $server == "all") {
                CloudServerManager::getInstance()->stopAll($forcefully);
            } else {
                CloudServerManager::getInstance()->stop($server, $forcefully);
            }
            return true;
        })->addParameter(new MultipleTypesParameter("server", [
            new ServerParameter("server", false),
            new TemplateParameter("template", false),
            new ServerGroupParameter("group", false),
            new StringEnumParameter("all", ["all"], false, false)
        ], false))->addParameter(new BoolParameter("forcefully", true)));

        $this->registerSubCommand(SubCommand::fromClosure("send", function (ICommandSender $sender, string $label, array $args): bool {
            /** @var CloudServer $server */
            $server = $args["server"];
            $commandLine = $args["commandLine"];
            $server->executeCommand($commandLine)->then(function (ServerCommandExecutionResult $result) use($sender, $server): void {
                $sender->success("Successfully ran the command on §b{}§r, server responded with the following messages:", $server->getName());
                if (empty($result->getMessages())) $sender->success("§cNone");
                foreach ($result->getMessages() as $message) {
                    $sender->success($message);
                }
            })->failure(fn() => $sender->warn("The command execution request ran out. §8(§b{}§8)", $commandLine));
            return true;
        })->addParameter(new ServerParameter("server", false))->addParameter(new StringParameter("commandLine", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("list", function (ICommandSender $sender, string $label, array $args): bool {
            $template = $args["template"] ?? null;
            if (empty(CloudServerManager::getInstance()->getAll($template))) {
                $sender->success("§cNo servers running.");
            } else {
                $sender->info("Servers §8(§b{}§8/§b{}§8)§r:", count($servers = CloudServerManager::getInstance()->getAll($template)), $template?->getName() ?? "All");
                foreach ($servers as $server) {
                    $sender->info(FormatUtils::implodeWithKeys(
                        array_diff_key($server->write(), ["uuid" => null, "id" => null, "internalStorage" => null, "path" => null]),
                        " §8| §r",
                        "§r: §b",
                        fn(string $key) => ucfirst($key)
                    ));
                }
            }

            return true;
        })->addParameter(new MultipleTypesParameter("template", [
            new TemplateParameter("template", true),
            new ServerGroupParameter("group", true)
        ], true)));

        $this->registerSubCommand(SubCommand::fromClosure("info", function (ICommandSender $sender, string $label, array $args): bool {
            /** @var CloudServer $server */
            $server = $args["server"];
            $formatted = FormatUtils::implodeWithKeys(
                array_merge($server->write(), ["path" => $server->getPath(), "channel" => $server->getServerClient()?->getAddress()]),
                "\n",
                "§r: §b",
                fn(string $key) => ucfirst($key),
                fn(string $key, mixed $value) => is_array($value) ? FileUtils::encodeJson($value, JSON_INVALID_UTF8_IGNORE | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) : $value
            );

            foreach (explode("\n", $formatted) as $line) {
                $sender->info($line);
            }

            return true;
        })->addParameter(new ServerParameter("server", false)));

        $this->registerSubCommand(SubCommand::fromClosure("save", function (ICommandSender $sender, string $label, array $args): bool {
            /** @var CloudServer $server */
            $server = $args["server"];
            $sender->info("Saving §b{}§r...", $server->getName());
            $start = microtime(true) * 1000;
            CloudServerManager::getInstance()->save($server)->then(fn() => $sender->success("Successfully §asaved §b{}§r. §8(§rTook §b{}ms§8)", $server->getName(), round((microtime(true) * 1000) - $start, 3)))
                ->failure(fn(?string $reason) => $sender->warn("Failed to save §b{}§r: §c{}", $server->getName(), $reason ?? "No reason applied"));
            return true;
        })->addParameter(new ServerParameter("server", false)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }
}