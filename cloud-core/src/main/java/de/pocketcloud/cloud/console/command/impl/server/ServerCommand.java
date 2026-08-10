package de.pocketcloud.cloud.console.command.impl.server;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.parameter.def.*;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.console.screen.impl.ServerConsoleMonitorScreen;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.util.FormatUtils;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.shared.component.software.ServerSoftware;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;

@CommandDescription(name = "server", description = "Manage the cloud servers", aliases = {"srv", "service"})
public final class ServerCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("start", this::handleStartSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false))
                    .addParameter(new IntegerParameter("amount", true, i -> Math.clamp(i, 1, 100)));
        }));

        registerSubCommand(SubCommand.lambda("stop", this::handleStopSub, true, subCommand -> {
            subCommand.addParameter(new MultipleTypesParameter("server", false, new TemplateParameter("template", true),
                            new ServerParameter("server", true),
                            new TemplateTypeParameter("server", true),
                            new ServerGroupParameter("server_group", true),
                            new TemplateTypeParameter("template_type", true),
                            new SoftwareParameter("software", true)))
                    .addFlag(CommandFlag.shortFlag("f"));
        }));

        registerSubCommand(SubCommand.lambda("stopall", this::handleStopAllSub, true, subCommand -> {
            subCommand.addFlag(CommandFlag.shortFlag("f"))
                    .addFlag(CommandFlag.shortFlag("y"));
        }));

        registerSubCommand(SubCommand.lambda("dispatch", this::handleDispatchSub, true, subCommand -> {
            subCommand.addParameter(new ServerParameter("server", false))
                    .addParameter(new StringParameter("command", false, true));
        }));

        registerSubCommand(SubCommand.lambda("save", this::handleSaveSub, true, subCommand -> {
            subCommand.addParameter(new ServerParameter("server", false));
        }));

        registerSubCommand(SubCommand.lambda("info", this::handleInfoSub, true, subCommand -> {
            subCommand.addParameter(new ServerParameter("server", false));
        }));

        registerSubCommand(SubCommand.lambda("screen", this::handleScreenSub, true, subCommand -> {
            subCommand.addParameter(new ServerParameter("server", false));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true, subCommand -> {
            subCommand.addParameter(new MultipleTypesParameter("filter", true, new TemplateParameter("template", false),
                    new TemplateTypeParameter("template_type", true),
                    new ServerGroupParameter("server_group", true),
                    new SoftwareParameter("software", true)));
        }));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    public boolean handleStartSub(CommandSender sender, CommandContext ctx) {
        int amount = ctx.hasArg("amount") ? ctx.argInt("amount") : 1;
        PocketCloud.instance().servers().start(ctx.arg("template", Template.class), amount);
        return true;
    }

    public boolean handleStopSub(CommandSender sender, CommandContext ctx) {
        Object obj = ctx.arg("server");
        boolean forcefully = ctx.hasFlag("f");
        if (obj instanceof CloudServer server) {
            PocketCloud.instance().servers().stop(server, forcefully);
        } else if (obj instanceof Template template) {
            PocketCloud.instance().servers().stop(template, forcefully);
        } else if (obj instanceof TemplateType type) {
            PocketCloud.instance().servers().stop(type, forcefully);
        } else if (obj instanceof ServerGroup group) {
            PocketCloud.instance().servers().stop(group, forcefully);
        } else if (obj instanceof ServerSoftware software) {
            for (ICloudServer server : PocketCloud.instance().servers().query(q -> q.runningSoftware(software.name()))) {
                PocketCloud.instance().servers().stop(server, forcefully);
            }
        }
        return true;
    }

    public boolean handleStopAllSub(CommandSender sender, CommandContext ctx) {
        boolean sure = ctx.hasFlag("y");
        boolean forcefully = ctx.hasFlag("f");
        if (sure) {
            PocketCloud.instance().servers().stopAll(forcefully);
        } else {
            awaitConfirmation(sender, "Are you sure you want to §cstop §rall servers?")
                    .thenSuccess(r -> {
                        if (r) {
                            PocketCloud.instance().servers().stopAll(forcefully);
                        }
                    });
        }
        return true;
    }

    public boolean handleDispatchSub(CommandSender sender, CommandContext ctx) {
        CloudServer server = ctx.arg("server", CloudServer.class);
        String command = ctx.arg("command", String.class);
        server.dispatch(command).thenSuccess(res -> {
            sender.success("Successfully ran the command on §b{}§r, server responded with the following messages:", server.name());
            if (res.messages().isEmpty()) sender.success("§cNone");
            for (String message : res.messages()) {
                for (String part : message.split("\n")) {
                    sender.success(part.trim());
                }
            }
        }).failure(t -> sender.error("Failed to dispatch command: §e{}", t.getMessage()));
        return true;
    }

    public boolean handleSaveSub(CommandSender sender, CommandContext ctx) {
        CloudServer server = ctx.arg("server", CloudServer.class);
        sender.info("Saving §b{}§r...", server.name());
        Instant start = Instant.now();
        server.save().thenSuccess(_ -> sender.success("Successfully §asaved §b{}§r. §8(§rTook §b{}ms§8)", server.name(), NumberUtils.formatNumber(Duration.between(start, Instant.now()).toMillis() / 1000F, 3)))
                .failure(e -> sender.error("Failed to save §b{}§r: §c{}", server.name(), e.getMessage()));
        return true;
    }

    public boolean handleInfoSub(CommandSender sender, CommandContext ctx) {
        CloudServer server = ctx.arg("server", CloudServer.class);
        sender.info("Name§8: §b{} §8(§b{}§8/§b{}§8)", server.name(), server.id(), server.templateName());
        sender.info("UUID§8: §b{}", server.uuid().toString());
        sender.info("Status§8: §b{} §8(§e{}§8)", server.status().getDisplay(), server.verificationStatus().name());
        sender.info("Port§8: §b{}", server.data().port());
        sender.info("Players§8: §b{}§8/§c{}", server.playerCount(), server.data().maxPlayers());
        sender.info("TPS§8: §b{} §8(§rAverage: §b{}§8)", FormatUtils.tps(server.data().tps(), true), FormatUtils.tps(server.data().avgTps(), true));
        sender.info("Memory Usage§8: §b{}§8/§c{} §8(§rPeak: §b{}§8)", FormatUtils.bytes(server.data().memoryUsage(), true), FormatUtils.bytes(server.data().memoryLimit(), false), FormatUtils.bytes(server.data().memoryPeak(), true));
        sender.info("CPU Usage§8: §b{}", FormatUtils.usagePercentage(server.data().cpuUsage(), true));
        sender.info("Used Software§8: §b{}", server.template().serverSoftware().name());
        sender.info("Path§8: §b{}", server.path().toAbsolutePath().toString());
        sender.info("Channel§8: §b{}", server.client().isPresent() ? server.client().get().address().toString() : "No channel yet");
        return true;
    }

    public boolean handleScreenSub(CommandSender sender, CommandContext ctx) {
        CloudServer server = ctx.arg("server", CloudServer.class);
        PocketCloud.instance().screens().set(new ServerConsoleMonitorScreen(server.name()));
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        ServerSearchQuery query = ServerSearchQuery.create();
        Object filter = ctx.arg("filter");
        if (filter instanceof Template template) {
            query.ofTemplate(template);
        } else if (filter instanceof TemplateType type) {
            query.ofType(type);
        } else if (filter instanceof ServerGroup group) {
            query.inGroup(group);
        } else if (filter instanceof ServerSoftware software) {
            query.runningSoftware(software.name());
        }

        Collection<ICloudServer> servers = PocketCloud.instance().servers().query(query).stream().sorted(Comparator.comparingInt(ICloudServer::playerCount)).toList();
        sender.info("Servers §8(§b{}§8/§b{}§8)§r:", servers.size(), PocketCloud.instance().servers().getAll().size());
        if (servers.isEmpty()) sender.info("§cNo servers found.");
        for (ICloudServer server : servers) {
            sender.info("Name§8: §b{} §8| §rPlayers§8: §b{}§8/§c{} §8| §rPort§8: §b{} §8| §rStatus§8: §b{} §8| §rSoftware§8: §b{} §8(§b{}§8)", server.name(),
                    server.playerCount(), server.data().maxPlayers(), server.data().port(), server.status().getDisplay(),
                    server.template().serverSoftware().name(), server.template().templateType().name());
        }

        return true;
    }
}