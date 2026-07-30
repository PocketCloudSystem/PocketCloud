package de.pocketcloud.bridge.platform.pnx.command;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.language.LanguageKey;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.data.CommandParameter;
import org.powernukkitx.command.tree.ParamList;
import org.powernukkitx.command.utils.CommandLogger;

import java.util.Map;
import java.util.Optional;

public final class TransferCommand extends Command {

    public TransferCommand() {
        super("transfer", LanguageKey.INGAME_COMMAND_DESCRIPTION_TRANSFER.translate(), "/transfer <server> [target]");
        setPermission("pocketcloud.command.transfer");

        this.commandParameters.clear();
        this.commandParameters.put("default",
                new CommandParameter[]{
                        CommandParameter.newType("server", false, CommandParamType.ID),
                        CommandParameter.newType("target", true, CommandParamType.ID),
                });

        this.enableParamTree();
    }

    @Override
    public int execute(CommandSender sender, String commandLabel, Map.Entry<String, ParamList> result, CommandLogger log) {
        if (testPermissionSilent(sender)) {
            var list = result.getValue();
            Optional<ICloudServer> server = CloudAPI.instance().servers().get(list.getResult(0).toString());
            if (server.isEmpty()) {
                sender.sendMessage(LanguageKey.INGAME_SERVER_NOT_FOUND.translate());
                return 0;
            }

            Optional<ICloudPlayer> target = sender instanceof Player p ? CloudAPI.instance().players().get(p.getName()) : Optional.empty();
            if (list.hasResult(1)) {
                String targetName = list.getResult(1);
                target = CloudAPI.instance().players().get(targetName);
            }

            if (target.isEmpty()) {
                sender.sendMessage(LanguageKey.INGAME_PLAYER_NOT_FOUND.translate());
                return 0;
            }

            ICloudServer actualServer = server.get();
            ICloudPlayer actualTarget = target.get();
            if (sender.getName().equals(actualTarget.name())) {
                sender.sendMessage(LanguageKey.INGAME_SERVER_CONNECT.translate(actualServer.name()));
            } else {
                sender.sendMessage(LanguageKey.INGAME_SERVER_TARGET_CONNECT.translate(actualTarget.name(), actualServer.name()));
                actualTarget.sendMessage(LanguageKey.INGAME_SERVER_CONNECT.translate(actualServer.name()));
            }

            if (!CloudAPI.instance().playerExecutor().transfer(actualTarget.name(), actualServer, true)) {
                if (sender.getName().equals(actualTarget.name())) {
                    sender.sendMessage(LanguageKey.INGAME_SERVER_CONNECT_FAILED.translate(actualServer.name()));
                } else {
                    sender.sendMessage(LanguageKey.INGAME_SERVER_TARGET_CONNECT_FAILED.translate(actualTarget.name(), actualServer.name()));
                    actualTarget.sendMessage(LanguageKey.INGAME_SERVER_CONNECT_FAILED.translate(actualServer.name()));
                }
            }
        } else {
            sender.sendMessage(LanguageKey.INGAME_NO_PERMISSION.translate());
        }
        return 1;
    }
}