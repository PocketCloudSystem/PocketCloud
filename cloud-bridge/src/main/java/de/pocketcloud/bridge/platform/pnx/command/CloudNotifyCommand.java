package de.pocketcloud.bridge.platform.pnx.command;

import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.bridge.cache.NotificationListCache;
import de.pocketcloud.common.cache.LocalCache;
import org.powernukkitx.Player;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;

public final class CloudNotifyCommand extends Command {

    public CloudNotifyCommand() {
        super("cloudnotify", LanguageKey.INGAME_COMMAND_DESCRIPTION_CLOUD_NOTIFY.translate(), "/cloudnotify");
        setPermission("pocketcloud.command.notify");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player player) {
            if (testPermissionSilent(player)) {
                if (LocalCache.get(NotificationListCache.class).contains(player.getName())) {
                    player.sendMessage(LanguageKey.INGAME_NOTIFY_DEACTIVATED.translate());
                    LocalCache.get(NotificationListCache.class).remove(player.getName());
                } else {
                    player.sendMessage(LanguageKey.INGAME_NOTIFY_ACTIVATED.translate());
                    LocalCache.get(NotificationListCache.class).add(player.getName(), true);
                }
            } else {
                player.sendMessage(LanguageKey.INGAME_NO_PERMISSION.translate());
            }
        }
        return true;
    }
}