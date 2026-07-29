package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTimingsSummary;

import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@CommandDescription(name = "timings", description = "View the cloud's timings")
public final class TimingsCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("paste", this::handlePasteSub));
        registerSubCommand(SubCommand.lambda("dump", this::handleDumpSub));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return false;
    }

    public boolean handlePasteSub(CommandSender sender, CommandContext ctx) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_z");
        Path path = PocketCloudPaths.storage().timings().with(formatter.format(new Date()) + ".txt").asPath();
        sender.info("§aPasting §rtimings into §b{}§r...", path.toAbsolutePath().toString());
        if (Benchmark.writeTimings(path, true)) {
            sender.success("Successfully §apasted §rtimings.");
        } else {
            sender.error("Failed to paste timings.");
        }
        return true;
    }

    public boolean handleDumpSub(CommandSender sender, CommandContext ctx) {
        List<BenchmarkTimingsSummary> timings = Benchmark.getSummary(Comparator.comparingDouble(BenchmarkTimingsSummary::max));
        for (BenchmarkTimingsSummary timing : timings) {
            sender.info(timing.format(3, true));
        }
        return true;
    }
}