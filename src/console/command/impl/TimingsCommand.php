<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\util\benchmark\Benchmark;
use pocketcloud\cloud\util\benchmark\BenchmarkTimingsSummary;
use pocketcloud\cloud\util\PathUtils;
use const pocketcloud\TIMINGS_PATH;

final class TimingsCommand extends Command {

    public function __construct() {
        parent::__construct("timings", "Manage the timings");
        $this->enableUseRegularHandlerForSubCommands();
        $this->registerSubCommand(SubCommand::nonHandler("paste"));
        $this->registerSubCommand(SubCommand::nonHandler("dump"));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        if ($subCommand?->getName() == "paste") {
            $sender->info("pasing timings into §b{}§r...", $path = PathUtils::join(TIMINGS_PATH, date("Y-m-d_H:i:s_T") . ".txt"));
            if (Benchmark::writeTimings($path)) {
                $sender->success("Successfully §apasted §rtimings.");
            } else $sender->warn("Failed to paste timings.");
        } else if ($subCommand?->getName() == "dump") {
            /** @var BenchmarkTimingsSummary $summary */
            foreach (Benchmark::getSummary() as $summary) {
                $sender->info($summary->format());
            }
        }
        return true;
    }
}