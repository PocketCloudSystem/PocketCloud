package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.console.command.parameter.def.SuggestingParameter;
import de.pocketcloud.cloud.backup.BackupService;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.parameter.def.StringParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@CommandDescription(name = "backup", description = "Back up and restore templates, groups and configs", aliases = {"backups"})
public final class BackupCommand extends Command {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("create", this::handleCreateSub, true));
        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true));
        registerSubCommand(SubCommand.lambda("restore", this::handleRestoreSub, true, subCommand -> {
            subCommand.addParameter(new SuggestingParameter("backup", false, BackupCommand::backupNames))
                    .addFlag(CommandFlag.shortFlag("y"));
        }));
    }

    public boolean handleCreateSub(CommandSender sender, CommandContext ctx) {
        sender.info("Creating a backup...");
        Thread.ofVirtual().start(() -> {
            try {
                Path zip = BackupService.create("backup");
                sender.success("Created the backup §b{} §8(§b{} KB§8)§r.", zip.getFileName(), Files.size(zip) / 1024);
            } catch (Exception e) {
                sender.error("Could not create the backup§8: §c{}", e.getMessage());
            }
        });
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        try {
            List<Path> backups = BackupService.list();
            if (backups.isEmpty()) {
                sender.info("There are no backups yet. Create one with §bbackup create§r.");
                return true;
            }
            sender.info("Backups §8(§b{}§8)§r, newest first:", backups.size());
            for (Path zip : backups) {
                sender.info("§b{} §8| §r{} §8| §r{} KB", zip.getFileName(), TIME.format(Instant.ofEpochMilli(zip.toFile().lastModified())), Files.size(zip) / 1024);
            }
        } catch (Exception e) {
            sender.error("Could not list the backups§8: §c{}", e.getMessage());
        }
        return true;
    }

    public boolean handleRestoreSub(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("backup");
        Runnable restore = () -> Thread.ofVirtual().start(() -> {
            try {
                String before = BackupService.restore(name);
                sender.success("Restored §b{}§r. The previous state was saved as §b{}§r.", name, before);
                sender.info("Templates and groups are synced to the other nodes. Restart this node to load the restored configs.");
            } catch (Exception e) {
                sender.error("Could not restore §b{}§8: §c{}", name, e.getMessage());
            }
        });
        if (ctx.hasFlag("y")) restore.run();
        else awaitConfirmation(sender, "Restore §b" + name + "§r? Templates and groups are §creplaced §r(in a cluster on all nodes), configs are overwritten.")
                .thenSuccess(r -> {
                    if (r) restore.run();
                });
        return true;
    }

    private static List<String> backupNames() {
        try {
            return BackupService.list().stream().map(p -> p.getFileName().toString()).toList();
        } catch (Exception e) {
            return List.of();
        }
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return false;
    }
}
