package de.pocketcloud.cloud.console.command.impl.player;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.search.PlayerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.*;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.shared.component.software.ServerSoftware;
import de.pocketcloud.shared.network.packet.type.TextType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

@CommandDescription(name = "player", description = "Manage the online players")
public final class PlayerCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("kick", this::handleKickSub, true, subCommand -> {
            subCommand.addParameter(new PlayerParameter("player", false))
                    .addParameter(new StringParameter("reason", true));
        }));

        registerSubCommand(SubCommand.lambda("text", this::handleTextSub, true, subCommand -> {
            subCommand.addParameter(new PlayerParameter("player", false))
                    .addParameter(new StringEnumParameter("type", false, Arrays.stream(TextType.values()).map(t -> t.name().toLowerCase()).toArray(String[]::new)))
                    .addParameter(new StringParameter("text", false, true));
        }));

        registerSubCommand(SubCommand.lambda("transfer", this::handleTransferSub, true, subCommand -> {
            subCommand.addParameter(new PlayerParameter("player", false))
                    .addParameter(new ServerParameter("server", false));
        }));

        registerSubCommand(SubCommand.lambda("info", this::handleInfoSub, true, subCommand -> {
            subCommand.addParameter(new PlayerParameter("player", false));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true, subCommand -> {
            subCommand.addParameter(new MultipleTypesParameter("filter", true, new TemplateParameter("template", false),
                    new TemplateTypeParameter("template_type", true),
                    new ServerGroupParameter("server_group", true),
                    new ServerParameter("server", false),
                    new SoftwareParameter("software", true)));
        }));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    public boolean handleKickSub(CommandSender sender, CommandContext ctx) {
        CloudPlayer player = ctx.arg("player", CloudPlayer.class);
        String reason = ctx.arg("reason", String.class, "No reason provided.");
        player.kick(reason);
        sender.success("Kicked §b{} §rfrom the server.", player.name());
        return true;
    }

    public boolean handleTextSub(CommandSender sender, CommandContext ctx) {
        CloudPlayer player = ctx.arg("player", CloudPlayer.class);
        TextType type = TextType.valueOf(ctx.argString("type").toUpperCase());
        String text = ctx.arg("text", String.class);
        sender.success("Sent a text from type §b{} §rto §b{}§r.", type.name(), player.name());
        if (type == TextType.MESSAGE) {
            player.sendMessage(text);
        } else if (type == TextType.TITLE) {
            player.sendTitle(text);
        } else if (type == TextType.POPUP) {
            player.sendPopup(text);
        } else if (type == TextType.TIP) {
            player.sendTip(text);
        } else if (type == TextType.ACTION_BAR) {
            player.sendActionbarMessage(text);
        } else if (type == TextType.TOAST) {
            String[] parts = text.split("\n");
            String title = parts[0];
            String body = String.join("\n", Arrays.copyOfRange(parts, 1, parts.length));
            player.sendToast(title, body);
        }
        return true;
    }

    public boolean handleTransferSub(CommandSender sender, CommandContext ctx) {
        CloudPlayer player = ctx.arg("player", CloudPlayer.class);
        CloudServer server = ctx.arg("server", CloudServer.class);
        player.transfer(server);
        sender.success("Transferred §b{} §rto §b{}§r.", player.name(), server.name());
        return true;
    }

    public boolean handleInfoSub(CommandSender sender, CommandContext ctx) {
        CloudPlayer player = ctx.arg("player", CloudPlayer.class);
        sender.info("Name§8: §b{}", player.name());
        sender.info("UniqueId§8: §b{}", player.uniqueId().toString());
        sender.info("XUID§8: §b{}", player.xboxUserId());
        sender.info("CurrentServer§8: §b{}", Objects.requireNonNullElse(player.currentServerName(), "§cNo server"));
        sender.info("CurrentProxy§8: §b{}", Objects.requireNonNullElse(player.currentProxyName(), "§cNo server"));
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        PlayerSearchQuery query = PlayerSearchQuery.create();
        Object filter = ctx.arg("filter");
        if (filter instanceof CloudServer server) {
            query.onServer(server);
        } else if (filter instanceof Template template) {
            query.ofTemplate(template);
        } else if (filter instanceof TemplateType type) {
            query.ofType(type);
        } else if (filter instanceof ServerGroup group) {
            query.inGroup(group);
        } else if (filter instanceof ServerSoftware software) {
            query.runningSoftware(software.name());
        }

        Collection<ICloudPlayer> players = PocketCloud.instance().players().query(query);
        sender.info("Players §8(§b{}§8/§b{}§8)§r:", players.size(), PocketCloud.instance().players().playerCount());
        if (players.isEmpty()) sender.info("§cNo players found.");
        for (ICloudPlayer player : players) {
            sender.info("Name§8: §b{} §8| §rCurrentServer§8: §b{} §8| §rCurrentProxy§8: §b{}", player.name(),
                    Objects.requireNonNullElse(player.currentServerName(), "§cNo server"),
                    Objects.requireNonNullElse(player.currentProxyName(), "§cNo server"));
        }
        return true;
    }
}