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
use pocketcloud\cloud\console\screen\impl\ServerConsoleMonitorScreen;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\network\packet\data\ServerCommandExecutionResult;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\server\util\ServerStatus;
use pocketcloud\cloud\util\FileUtils;
use pocketcloud\cloud\util\FormatUtils;

final class ServerCommand extends Command {

    public function __construct() {
        parent::__construct("server", "Manage the cloud's servers", ["srv"]);

        $this->registerSubCommand(SubCommand::fromClosure("start", $this->handleStartSub(...), ["start"])
            ->addParameter(new TemplateParameter("template", false))
            ->addParameter(new IntegerParameter("amount", true, function (int $number): int {
                if ($number < 0) return 1;
                return min(50, $number);
            })));

        $this->registerSubCommand(SubCommand::fromClosure("stop", $this->handleStopSub(...), ["stop"])
            ->addParameter(new MultipleTypesParameter("server", [
                new ServerParameter("server", false),
                new TemplateParameter("template", false),
                new ServerGroupParameter("group", false),
                new StringEnumParameter("all", ["all"], false, false)
            ], false))
            ->addParameter(new BoolParameter("forcefully", true)));

        $this->registerSubCommand(SubCommand::fromClosure("send", $this->handleSendSub(...), ["send", "execute"])
            ->addParameter(new ServerParameter("server", false))
            ->addParameter(new StringParameter("commandLine", false, true)));

        $this->registerSubCommand(SubCommand::fromClosure("list", $this->handleListSub(...))
            ->addParameter(new MultipleTypesParameter("template", [
                new TemplateParameter("template", true),
                new ServerGroupParameter("group", true)
            ], true)));

        $this->registerSubCommand(SubCommand::fromClosure("info", $this->handleInfoSub(...))
            ->addParameter(new ServerParameter("server", false)));

        $this->registerSubCommand(SubCommand::fromClosure("save", $this->handleSaveSub(...), ["save"])
            ->addParameter(new ServerParameter("server", false)));

        $this->registerSubCommand(SubCommand::fromClosure("screen", $this->handleScreenSub(...), ["screen"])
            ->addParameter(new ServerParameter("server", false)));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }

    private function handleStartSub(ICommandSender $sender, string $label, array $args): bool {
        $template = $args["template"];
        $amount = $args["amount"] ?? 1;
        CloudServerManager::getInstance()->start($template, $amount);
        return true;
    }

    private function handleStopSub(ICommandSender $sender, string $label, array $args): bool {
        $server = $args["server"];
        $forcefully = $args["forcefully"] ?? false;

        if (is_string($server) && $server == "all") CloudServerManager::getInstance()->stopAll($forcefully);
        else CloudServerManager::getInstance()->stop($server, $forcefully);
        return true;
    }

    private function handleSendSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudServer $server */
        $server = $args["server"];
        $commandLine = $args["commandLine"];
        $server->executeCommand($commandLine)->then(function (ServerCommandExecutionResult $result) use($sender, $server): void {
            $sender->success("Successfully ran the command on §b{}§r, server responded with the following messages:", $server->getName());
            if (empty($result->getMessages())) $sender->success("§cNone");
            foreach ($result->getMessages() as $message) {
                foreach (explode("\n", $message) as $msgPart) {
                    $sender->success(trim($msgPart));
                }
            }
        })->failure(fn() => $sender->warn("The command execution request ran out. §8(§b{}§8)", $commandLine));
        return true;
    }

    private function handleListSub(ICommandSender $sender, string $label, array $args): bool {
        $template = $args["template"] ?? null;
        if (empty(CloudServerManager::getInstance()->getAll($template))) $sender->info("§cNo servers running.");
        $sender->info("Servers §8(§b{}§8/§b{}§8)§r:", count($servers = CloudServerManager::getInstance()->getAll($template)), $template?->getName() ?? "All");
        foreach ($servers as $server) {
            $sender->info(FormatUtils::implodeWithKeys(
                $server->write(),
                " §8| §r",
                "§8: §b",
                fn(string $key) => ucfirst($key),
                function (string $key, mixed $value) use ($server): mixed {
                    if ($key == "serverStatus") {
                        return ServerStatus::fromName($value)?->getDisplay() ?? $value;
                    } else if ($key == "template") {
                        return $server->getTemplateName() . ($server->getTemplate()?->getParentServerGroup() !== null ? " §8(§e" . $server->getTemplate()->getParentServerGroup()->getName() . "§8)" : "");
                    }

                    return $value;
                },
                "uuid", "id", "internalStorage", "tps", "avgTps", "memoryUsage", "memoryPeak", "memoryLimit", "cpuUsage"
            ));
        }

        return true;
    }

    private function handleInfoSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudServer $server */
        $server = $args["server"];
        $formatted = FormatUtils::implodeWithKeys(
            array_merge($server->write(), ["path" => $server->getPath(), "channel" => $server->getServerClient()?->getAddress()]),
            "\n",
            "§8: §b",
            function (string $key): string {
                $potentialNewKey = preg_replace("/(?<!^)([A-Z])/", " $1", ucfirst($key));
                return match ($key) {
                    "serverStatus" => "Status",
                    "avgTps" => "Average TPS",
                    default => $potentialNewKey
                };
            },
            function (string $key, mixed $value) use($server): mixed {
                if (is_array($value)) return FileUtils::encodeJson($value, JSON_INVALID_UTF8_IGNORE | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);

                if ($key == "name") {
                    return "§b" . $value . " §8(§b" . $server->getId() . "§8/§b" . $server->getTemplateName() . "§8)";
                } else if ($key == "tps" || $key == "avgTps") {
                    return FormatUtils::tps($value);
                } else if (str_contains($key, "memory")) {
                    return FormatUtils::bytes($value);
                } else if (str_contains($key, "Usage")) {
                    return FormatUtils::usagePercentage($value);
                } else if ($key == "serverStatus") {
                    return $server->getServerStatus()?->getDisplay() ?? "Not Applied";
                }

                return $value;
            },
            "template", "id"
        );

        foreach (explode("\n", $formatted) as $line) {
            $sender->info($line);
        }

        return true;
    }

    private function handleSaveSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudServer $server */
        $server = $args["server"];
        $sender->info("Saving §b{}§r...", $server->getName());
        $start = microtime(true) * 1000;
        CloudServerManager::getInstance()->save($server)->then(fn() => $sender->success("Successfully §asaved §b{}§r. §8(§rTook §b{}ms§8)", $server->getName(), round((microtime(true) * 1000) - $start, 3)))
            ->failure(fn(?string $reason) => $sender->warn("Failed to save §b{}§r: §c{}", $server->getName(), $reason ?? "No reason applied"));
        return true;
    }

    private function handleScreenSub(ICommandSender $sender, string $label, array $args): bool {
        /** @var CloudServer $server */
        $server = $args["server"];
        ScreenManager::getInstance()->setCurrentScreen(new ServerConsoleMonitorScreen($server->getName()));
        return true;
    }
}