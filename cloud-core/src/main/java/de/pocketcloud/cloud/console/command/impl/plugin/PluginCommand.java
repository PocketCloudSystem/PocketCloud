package de.pocketcloud.cloud.console.command.impl.plugin;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.DisabledPluginParameter;
import de.pocketcloud.cloud.console.command.parameter.def.EnabledPluginParameter;
import de.pocketcloud.cloud.console.command.parameter.def.PluginParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.plugin.CloudPlugin;
import de.pocketcloud.cloud.plugin.CloudPluginState;

import java.util.Collection;
import java.util.List;

@CommandDescription(name = "plugin", description = "Manage the plugins", aliases = {"pl"})
public final class PluginCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("enable", this::handleEnableSub, true, subCommand -> {
            subCommand.addParameter(new DisabledPluginParameter("plugin", false));
        }));

        registerSubCommand(SubCommand.lambda("disable", this::handleDisableSub, true, subCommand -> {
            subCommand.addParameter(new EnabledPluginParameter("plugin", false));
        }));

        registerSubCommand(SubCommand.lambda("info", this::handleInfoSub, true, subCommand -> {
            subCommand.addParameter(new PluginParameter("plugin", false));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    public boolean handleEnableSub(CommandSender sender, CommandContext ctx) {
        PocketCloud.instance().plugins().enable(ctx.arg("plugin", CloudPlugin.class));
        return true;
    }

    public boolean handleDisableSub(CommandSender sender, CommandContext ctx) {
        PocketCloud.instance().plugins().disable(ctx.arg("plugin", CloudPlugin.class));
        return true;
    }

    public boolean handleInfoSub(CommandSender sender, CommandContext ctx) {
        CloudPlugin plugin = ctx.arg("plugin", CloudPlugin.class);
        int tasks = plugin.getScheduler().getAll().size();
        sender.info("Name§8: §b{}", plugin.getDescription().name());
        sender.info("Version§8: §b{}", plugin.getDescription().version());
        sender.info("State§8: §b{}", plugin.getState() == CloudPluginState.ENABLED ? "§aEnabled" : "§cDisabled");
        sender.info("Description§8: §b{}", plugin.getDescription().description().isBlank() ? "Empty" :  plugin.getDescription().description());
        sender.info("Main§8: §b{}", plugin.getDescription().main());
        sender.info("Authors§8: §c{}", String.join("§8, §c", plugin.getDescription().authors().isEmpty() ? List.of("None") : plugin.getDescription().authors()));
        sender.info("Data Folder§8: §b", plugin.getDataFolder().toAbsolutePath().toString());
        sender.info("Plugin File Path§8: §b", plugin.getPluginFilePath().toAbsolutePath().toString());
        sender.info("Scheduled Tasks§8: §b{} tasks", tasks);
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        Collection<CloudPlugin> plugins = PocketCloud.instance().plugins().getPlugins().values();
        sender.info("Plugins §8(§b{}§8)§r:", plugins.size());
        if (plugins.isEmpty()) sender.info("§cNo plugins found.");
        for (CloudPlugin plugin : plugins) {
            sender.info("Full Name§8: §b{} §8| §rState: §b{}", plugin.getDescription().name() + "@" + plugin.getDescription().version(), plugin.getState() == CloudPluginState.ENABLED ? "§aEnabled" : "§cDisabled");
        }
        return true;
    }
}