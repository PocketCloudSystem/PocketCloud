package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.StringParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.scheduler.CommandScheduler;

import java.util.List;

@CommandDescription(name = "schedule", description = "Run console commands at cron times", aliases = {"schedules"})
public final class ScheduleCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("add", this::handleAddSub, true, sub -> sub
                .addParameter(new StringParameter("cron", false))
                .addParameter(new StringParameter("command", false))));
        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true));
        registerSubCommand(SubCommand.lambda("remove", this::handleRemoveSub, true, sub -> sub
                .addParameter(new StringParameter("id", false))));
    }

    public boolean handleAddSub(CommandSender sender, CommandContext ctx) {
        try {
            CommandScheduler.Schedule schedule = CommandScheduler.add(ctx.argString("cron"), ctx.argString("command"));
            sender.success("Scheduled §b{} §rat §b{}§r §8(id §b{}§8)§r.", schedule.command(), schedule.cron(), schedule.id());
        } catch (IllegalArgumentException e) {
            sender.error("§cInvalid schedule§8: §c{}", e.getMessage());
            sender.info("Example§8: §bschedule add \"0 4 * * *\" \"template restart Lobby\"");
        }
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        List<CommandScheduler.Schedule> schedules = CommandScheduler.all();
        if (schedules.isEmpty()) {
            sender.info("There are no scheduled commands. Add one with §bschedule add \"<cron>\" \"<command>\"§r.");
            return true;
        }
        sender.info("Scheduled commands (§b{}§r):", schedules.size());
        for (CommandScheduler.Schedule s : schedules) sender.info("§b{} §8| §r{} §8| §b{}", s.id(), s.cron(), s.command());
        return true;
    }

    public boolean handleRemoveSub(CommandSender sender, CommandContext ctx) {
        String id = ctx.argString("id");
        if (CommandScheduler.remove(id)) sender.success("Removed the scheduled command §b{}§r.", id);
        else sender.error("§cThere is no scheduled command with the id §b{}§c.", id);
        return true;
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return false;
    }
}
