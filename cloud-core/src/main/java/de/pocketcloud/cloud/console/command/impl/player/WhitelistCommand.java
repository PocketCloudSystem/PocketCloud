package de.pocketcloud.cloud.console.command.impl.player;

import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.TabComplete;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.StringParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.common.cache.LocalCache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@CommandDescription(name = "whitelist", description = "Manage the whitelist", aliases = {"maintenance", "wl"})
public final class WhitelistCommand extends Command implements TabComplete {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("add", this::handleAddSub, true, subCommand -> {
            subCommand.addParameter(new StringParameter("player", false, true));
        }));

        registerSubCommand(SubCommand.lambda("remove", this::handleRemoveSub, true, subCommand -> {
            subCommand.addParameter(new StringParameter("player", false, true));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    public boolean handleAddSub(CommandSender sender, CommandContext ctx) {
        String playerName = ctx.argString("player");
        if (!LocalCache.get(WhitelistCache.class).contains(playerName)) {
            CloudProvider.current().addToWhitelist(playerName);
            sender.success("Successfully §aadded §b{} §rto the §bwhitelist§r.", playerName);
        } else {
            sender.warn("The player §b{} §ris §calready §ron the whitelist.", playerName);
        }
        return true;
    }

    public boolean handleRemoveSub(CommandSender sender, CommandContext ctx) {
        String playerName = ctx.argString("player");
        if (LocalCache.get(WhitelistCache.class).contains(playerName)) {
            CloudProvider.current().removeFromWhitelist(playerName);
            sender.success("Successfully §cremoved §b{} §rto the §bwhitelist§r.", playerName);
        } else {
            sender.warn("The player §b{} §ris §cnot §ron the whitelist.", playerName);
        }
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        List<String> players = LocalCache.get(WhitelistCache.class).getAll().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
        sender.info("Players §8(§b{}§8)§r:", players.size());
        if (players.isEmpty()) sender.info("§cNo players on the whitelist.");
        else sender.info("§b" + String.join("§8, §b", players));
        return true;
    }

    @Override
    public Collection<String> onTabComplete(List<String> args) {
        if (args.size() == 2) {
            if (args.getFirst().equals("remove")) {
                return LocalCache.get(WhitelistCache.class).getAll().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();
            }
        }
        return List.of();
    }
}