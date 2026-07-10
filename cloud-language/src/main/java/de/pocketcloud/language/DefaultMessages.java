package de.pocketcloud.language;

import java.util.LinkedHashMap;

public final class DefaultMessages {

    public static final LinkedHashMap<String, String> MESSAGES_EN = new LinkedHashMap<>();
    public static final LinkedHashMap<String, String> MESSAGES_GER = new LinkedHashMap<>();

    static {
        // raw
        MESSAGES_EN.put("raw.yes", "Yes");
        MESSAGES_EN.put("raw.no", "No");
        MESSAGES_EN.put("raw.author", "Author(s)");
        MESSAGES_EN.put("raw.description", "Description");
        MESSAGES_EN.put("raw.enabled", "Enabled");

        // commands
        MESSAGES_EN.put("inGame.command.description.cloud", "Manage the cloud");
        MESSAGES_EN.put("inGame.command.description.cloud_notify", "Enable/Disable notifications");
        MESSAGES_EN.put("inGame.command.description.hub", "Go to the lobby");
        MESSAGES_EN.put("inGame.command.description.cloudnpc", "Manage Cloud NPCs");
        MESSAGES_EN.put("inGame.command.description.template_group", "Manage template groups");
        MESSAGES_EN.put("inGame.command.description.transfer", "Connect to different servers");

        // general
        MESSAGES_EN.put("inGame.prefix", "§8[§l§3Pocket§bCloud§r§8] §r");
        MESSAGES_EN.put("inGame.no.permission", "{PREFIX}§cYou don't have permission to use this command!");
        MESSAGES_EN.put("inGame.template.kick.maintenance", "§cThis template is currently under maintenance.");

        // notify messages
        MESSAGES_EN.put("inGame.notify.message.server_starting", "{PREFIX}§fThe server §b%server% §fis §astarting§r...");
        MESSAGES_EN.put("inGame.notify.message.server_stopping", "{PREFIX}§fThe server §b%server% §fis §cstopping§r...");
        MESSAGES_EN.put("inGame.notify.message.server_timed_out", "{PREFIX}§fThe server §b%server% §fhas §ctimed out§r.");
        MESSAGES_EN.put("inGame.notify.message.server_stop_timed_out", "{PREFIX}§fFailed to stop the server §e%server%§f.");
        MESSAGES_EN.put("inGame.notify.message.server_crashed", "{PREFIX}§fThe server §b%server% §chas crashed!");
        MESSAGES_EN.put("inGame.notify.message.server_start_failed", "{PREFIX}§fThe server §b%server% §cfailed §fto start: §b%reason%");
        MESSAGES_EN.put("inGame.notify.message.player_joined", "{PREFIX}§fPlayer §b%player% §fhas §aconnected §fvia §b%server%§f.");
        MESSAGES_EN.put("inGame.notify.message.player_left", "{PREFIX}§fPlayer §b%player% §fhas §cdisconnected §ffrom §b%server%§f.");
        MESSAGES_EN.put("inGame.notify.message.player_join_failed", "{PREFIX}§fPlayer §b%player% §ftried to connect via §b%server%§f, but §cfailed§f: §b%reason%");
        MESSAGES_EN.put("inGame.notify.message.player_kicked", "{PREFIX}§fPlayer §b%player% §fhas been §ckicked §ffrom §b%server%§f: §b%reason%");
        MESSAGES_EN.put("inGame.notify.message.player_switched_server", "{PREFIX}§fPlayer §b%player% §fswitched servers §8(§b%old_server% §8-> §b%new_server%§8)");

        // server
        MESSAGES_EN.put("inGame.server.verified", "§rThe server was §averified §rby the cloud!");
        MESSAGES_EN.put("inGame.server.verify.denied", "§cThe verify request was denied by the cloud! Shutting down...");
        MESSAGES_EN.put("inGame.server.verify.failed", "§cThe verify request was not processed by the cloud! Shutting down...");
        MESSAGES_EN.put("inGame.network.receive.unknown", "§cAn unknown packet was sent by the cloud!");
        MESSAGES_EN.put("inGame.server.timeout", "§cThe cloud has interrupted the connection! Shutting down the server...");
        MESSAGES_EN.put("inGame.server.connect", "{PREFIX}§7Connecting to §e%0%§r...");
        MESSAGES_EN.put("inGame.server.already.connected", "{PREFIX}§cYou are already connected to §e%0%§c!");
        MESSAGES_EN.put("inGame.server.connect.failed", "{PREFIX}§cFailed to connect to §e%0%§c!");
        MESSAGES_EN.put("inGame.server.target.already.connected", "{PREFIX}§e%0% §cis already connected to server §e%1%§c!");
        MESSAGES_EN.put("inGame.server.target.connect", "{PREFIX}§e%0% §7is connecting to §e%1%§7...");
        MESSAGES_EN.put("inGame.server.target.connect.failed", "{PREFIX}§e%0% §ccould not be connected to §e%1%§c!");
        MESSAGES_EN.put("inGame.server.not.found", "{PREFIX}§cServer not found!");
        MESSAGES_EN.put("inGame.server.saved", "{PREFIX}§aServer has been saved!");

        // misc not-found / state
        MESSAGES_EN.put("inGame.template.not.found", "{PREFIX}§cTemplate not found!");
        MESSAGES_EN.put("inGame.player.not.found", "{PREFIX}§cPlayer not found!");
        MESSAGES_EN.put("inGame.already.in.lobby", "{PREFIX}§cYou are already in a lobby!");
        MESSAGES_EN.put("inGame.notify.activated", "{PREFIX}§aYou will now receive notifications from the §bCloud§a!");
        MESSAGES_EN.put("inGame.notify.deactivated", "{PREFIX}§cYou will no longer receive notifications from the §bCloud§c!");
        MESSAGES_EN.put("inGame.max.servers.reached", "{PREFIX}§cMaximum servers for template §e%0% §creached!");

        // text actions
        MESSAGES_EN.put("inGame.text.successful.message", "{PREFIX}§aYou have successfully sent a §emessage §ato §e%0%§a!");
        MESSAGES_EN.put("inGame.text.successful.popup", "{PREFIX}§aYou have successfully sent a §epopup §ato §e%0%§a!");
        MESSAGES_EN.put("inGame.text.successful.tip", "{PREFIX}§aYou have successfully sent a §etip §ato §e%0%§a!");
        MESSAGES_EN.put("inGame.text.successful.title", "{PREFIX}§aYou have successfully sent a §etitle §ato §e%0%§a!");
        MESSAGES_EN.put("inGame.text.successful.action_bar", "{PREFIX}§aYou have successfully sent an §eaction bar message §ato §e%0%§a!");
        MESSAGES_EN.put("inGame.text.successful.toast_notification", "{PREFIX}§aYou have successfully sent a §etoast notification §ato §e%0%§a!");
        MESSAGES_EN.put("inGame.console.log.successful", "{PREFIX}§aThe message was successfully sent to the §bCloud§a!");

        // modules
        MESSAGES_EN.put("inGame.module.enabled", "{PREFIX}§aThe module §e%0% §awas successfully enabled!");
        MESSAGES_EN.put("inGame.module.already.enabled", "{PREFIX}§cThe module §e%0% §cis already enabled!");
        MESSAGES_EN.put("inGame.module.disabled", "{PREFIX}§aThe module §e%0% §awas successfully disabled!");
        MESSAGES_EN.put("inGame.module.already.disabled", "{PREFIX}§cThe module §e%0% §cis already disabled!");
        MESSAGES_EN.put("inGame.module.no.enabled", "{PREFIX}§cNo modules are enabled!");
        MESSAGES_EN.put("inGame.module.no.disabled", "{PREFIX}§cNo modules are disabled!");

        // cloudnpc
        MESSAGES_EN.put("inGame.cloudnpc.created", "{PREFIX}§aCloud NPC was created!");
        MESSAGES_EN.put("inGame.cloudnpc.removed", "{PREFIX}§cCloud NPC was removed!");
        MESSAGES_EN.put("inGame.cloudnpc.select", "{PREFIX}§7Hit a Cloud NPC to remove it!");
        MESSAGES_EN.put("inGame.cloudnpc.process.cancelled", "{PREFIX}§cThe process was cancelled!");
        MESSAGES_EN.put("inGame.cloudnpc.quickjoin.no_server", "{PREFIX}§cNo server found!");
        MESSAGES_EN.put("inGame.cloudnpc.name_tag", "§e%1%\n§8» §7%0% playing.");
        MESSAGES_EN.put("inGame.cloudnpc.name_tag.maintenance", "§e%1% §8- §c§lMAINTENANCE§r\n§8» §7%0% playing.");

        // template groups
        MESSAGES_EN.put("inGame.template_group.exists", "{PREFIX}§cA template group with the id §e%0% §calready exists!");
        MESSAGES_EN.put("inGame.template_group.created", "{PREFIX}§aThe template group with the id §e%0% §awas created!");
        MESSAGES_EN.put("inGame.template_group.removed", "{PREFIX}§cThe template group with the id §e%0% §cwas removed!");

        // skin models
        MESSAGES_EN.put("inGame.skin_model.created", "{PREFIX}§aThe model with the id §e%0% §awas created!");
        MESSAGES_EN.put("inGame.skin_model.edited", "{PREFIX}§aThe model with the id §e%0% §awas edited!");
        MESSAGES_EN.put("inGame.skin_model.removed", "{PREFIX}§cThe model with the id §e%0% §cwas removed!");
        MESSAGES_EN.put("inGame.skin_model.failed", "{PREFIX}§cThe model with the id §e%0% §ccould not be created because the paths are incorrect!");
        MESSAGES_EN.put("inGame.skin_model.exists", "{PREFIX}§cThe model with the id §e%0% §calready exists!");

        // proxy
        MESSAGES_EN.put("inGame.proxy.stopped", "§f§lProxy shutting down...");

        // ui - general
        MESSAGES_EN.put("inGame.ui.general.selection.title", "§lSelection");
        MESSAGES_EN.put("inGame.ui.general.selection.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.general.selection.option.name", "§eSelect by name");
        MESSAGES_EN.put("inGame.ui.general.selection.option.selection", "§eSelect by dropdown");

        // ui - cloud main
        MESSAGES_EN.put("inGame.ui.cloud.main.title", "§3§lPocket§bCloud");
        MESSAGES_EN.put("inGame.ui.cloud.main.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.cloud.main.button.manage_server", "§eManage the servers");
        MESSAGES_EN.put("inGame.ui.cloud.main.button.manage_player", "§bManage the players");
        MESSAGES_EN.put("inGame.ui.cloud.main.button.manage_module", "§dManage the modules");
        MESSAGES_EN.put("inGame.ui.cloud.main.button.manage_template", "§6Manage the templates");
        MESSAGES_EN.put("inGame.ui.cloud.main.button.save_server", "§aSave the server");
        MESSAGES_EN.put("inGame.ui.cloud.main.button.cloud_log_console", "§fMessage to §bCloud §fconsole");

        // ui - cloud log console
        MESSAGES_EN.put("inGame.ui.cloud_log_console.title", "§bCloud Console");
        MESSAGES_EN.put("inGame.ui.cloud_log_console.element.message.text", "§7Message");
        MESSAGES_EN.put("inGame.ui.cloud_log_console.element.log_type.text", "§7Log type");

        // ui - manage module
        MESSAGES_EN.put("inGame.ui.manage_module.title", "§dManage Modules");
        MESSAGES_EN.put("inGame.ui.manage_module.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.manage_module.button.enable", "§aEnable a module");
        MESSAGES_EN.put("inGame.ui.manage_module.button.disable", "§cDisable a module");
        MESSAGES_EN.put("inGame.ui.manage_module.button.list", "§eList all modules");
        MESSAGES_EN.put("inGame.ui.manage_module.sub.enable.title", "§aEnable Module");
        MESSAGES_EN.put("inGame.ui.manage_module.sub.enable.name.text", "§7Module Name");
        MESSAGES_EN.put("inGame.ui.manage_module.sub.enable.dropdown.text", "§7Select Module");
        MESSAGES_EN.put("inGame.ui.manage_module.sub.disable.title", "§cDisable Module");
        MESSAGES_EN.put("inGame.ui.manage_module.sub.disable.name.text", "§7Module Name");
        MESSAGES_EN.put("inGame.ui.manage_module.sub.disable.dropdown.text", "§7Select Module");

        // ui - manage player
        MESSAGES_EN.put("inGame.ui.manage_player.title", "§bManage Players");
        MESSAGES_EN.put("inGame.ui.manage_player.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.manage_player.button.text", "§fSend a message");
        MESSAGES_EN.put("inGame.ui.manage_player.button.kick", "§cKick a player");
        MESSAGES_EN.put("inGame.ui.manage_player.button.list", "§eList all players");
        MESSAGES_EN.put("inGame.ui.manage_player.button.info", "§bInfo about a player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.text.title", "§fSend Message");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.text.name.text", "§7Player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.text.message.text", "§7Message");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.text.text_type.text", "§7Text type");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.text.dropdown.text", "§7Select Player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.kick.title", "§cKick Player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.kick.name.text", "§7Player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.kick.reason.text", "§7Reason");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.kick.dropdown.text", "§7Select Player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.info.title", "§bPlayer Info");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.info.name.text", "§7Player");
        MESSAGES_EN.put("inGame.ui.manage_player.sub.info.dropdown.text", "§7Select Player");

        // ui - manage server
        MESSAGES_EN.put("inGame.ui.manage_server.title", "§eManage Servers");
        MESSAGES_EN.put("inGame.ui.manage_server.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.manage_server.button.start", "§aStart a server");
        MESSAGES_EN.put("inGame.ui.manage_server.button.stop", "§cStop a server");
        MESSAGES_EN.put("inGame.ui.manage_server.button.list", "§eList all servers");
        MESSAGES_EN.put("inGame.ui.manage_server.button.info", "§bInfo about a server");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.start.title", "§aStart Server");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.start.name.text", "§7Template Name");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.start.count.text", "§7Number of servers");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.start.dropdown.text", "§7Select Template");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.stop.title", "§cStop Server");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.stop.name.text", "§7Server Name");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.stop.dropdown.text", "§7Select Server");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.stop.template_option.text", "§7Stop the whole template?");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.stop.all_option.text", "§7Stop all servers?");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.info.title", "§bServer Info");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.info.name.text", "§7Server Name");
        MESSAGES_EN.put("inGame.ui.manage_server.sub.info.dropdown.text", "§7Select Server");

        // ui - manage template
        MESSAGES_EN.put("inGame.ui.manage_template.title", "§6Manage Templates");
        MESSAGES_EN.put("inGame.ui.manage_template.button.info", "§bInfo about a template");
        MESSAGES_EN.put("inGame.ui.manage_template.button.list", "§eList all templates");
        MESSAGES_EN.put("inGame.ui.manage_template.sub.info.title", "§bTemplate Info");
        MESSAGES_EN.put("inGame.ui.manage_template.sub.info.dropdown.text", "§7Select Template");

        // kick
        MESSAGES_EN.put("inGame.kick.successful", "{PREFIX}§aYou have successfully kicked §e%0%§a!");

        // ui - cloudnpc
        MESSAGES_EN.put("inGame.ui.cloudnpc.main.title", "§eManage NPCs");
        MESSAGES_EN.put("inGame.ui.cloudnpc.main.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.cloudnpc.main.button.create", "§aCreate an NPC");
        MESSAGES_EN.put("inGame.ui.cloudnpc.main.button.remove", "§cRemove an NPC");
        MESSAGES_EN.put("inGame.ui.cloudnpc.main.button.list", "§eList all NPCs");
        MESSAGES_EN.put("inGame.ui.cloudnpc.main.button.models", "§6Manage skin models");
        MESSAGES_EN.put("inGame.ui.cloudnpc.create.title", "§aCreate NPC");
        MESSAGES_EN.put("inGame.ui.cloudnpc.create.element.name.text", "§7Template");
        MESSAGES_EN.put("inGame.ui.cloudnpc.create.element.model.text", "§7Skin model");
        MESSAGES_EN.put("inGame.ui.cloudnpc.create.element.headRotation.text", "§7Head rotation?");
        MESSAGES_EN.put("inGame.ui.cloudnpc.list.title", "§eNPC List");
        MESSAGES_EN.put("inGame.ui.cloudnpc.list.text", "§7Currently §e%0% §7NPC(s) available.");
        MESSAGES_EN.put("inGame.ui.cloudnpc.list_view.title", "§e%0%");
        MESSAGES_EN.put("inGame.ui.cloudnpc.list_view.button.teleport", "§aTeleport");
        MESSAGES_EN.put("inGame.ui.cloudnpc.list_view.button.back", "§cBack");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_server.title", "§e%0%");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_server.text", "§e%0% §7server(s) with the template §e%1% §7available.");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_server.no.server", "§cNo servers available");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_server.button.server", "§e%0%\n§a%1%§8/§c%2%");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_template.title", "§e%0%");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_template.text", "§7Choose a template from the group §e%0%§7.");
        MESSAGES_EN.put("inGame.ui.cloudnpc.choose_template.button.template", "§e%0%\n§a%1%§8/§c%2%");

        // ui - template group
        MESSAGES_EN.put("inGame.ui.template_group.main.title", "§6Manage Template Groups");
        MESSAGES_EN.put("inGame.ui.template_group.main.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.template_group.main.button.create", "§aCreate");
        MESSAGES_EN.put("inGame.ui.template_group.main.button.edit", "§6Edit");
        MESSAGES_EN.put("inGame.ui.template_group.main.button.remove", "§cRemove");
        MESSAGES_EN.put("inGame.ui.template_group.main.button.list", "§eList");
        MESSAGES_EN.put("inGame.ui.template_group.create.title", "§aCreate Template Group");
        MESSAGES_EN.put("inGame.ui.template_group.create.element.id.text", "§7Group ID");
        MESSAGES_EN.put("inGame.ui.template_group.create.element.display.text", "§7Display Name");
        MESSAGES_EN.put("inGame.ui.template_group.remove.title", "§cRemove Template Group");
        MESSAGES_EN.put("inGame.ui.template_group.remove.text", "§7Choose a template group");
        MESSAGES_EN.put("inGame.ui.template_group.edit_selection.title", "§6Select Group");
        MESSAGES_EN.put("inGame.ui.template_group.edit_selection.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.template_group.edit.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.template_group.edit.button.add_template", "§aAdd a template");
        MESSAGES_EN.put("inGame.ui.template_group.edit.button.remove_template", "§cRemove a template");
        MESSAGES_EN.put("inGame.ui.template_group.edit.button.change_display", "§fChange display name");
        MESSAGES_EN.put("inGame.ui.template_group.add_template.title", "§aAdd Template");
        MESSAGES_EN.put("inGame.ui.template_group.add_template.text", "§7Choose a template");
        MESSAGES_EN.put("inGame.ui.template_group.remove_template.title", "§cRemove Template");
        MESSAGES_EN.put("inGame.ui.template_group.remove_template.text", "§7Choose a template");
        MESSAGES_EN.put("inGame.ui.template_group.change_display.title", "§fEdit Display Name");
        MESSAGES_EN.put("inGame.ui.template_group.change_display.element.display", "§7New display name");

        // ui - skin model
        MESSAGES_EN.put("inGame.ui.skin_model.main.title", "§6Manage Skin Models");
        MESSAGES_EN.put("inGame.ui.skin_model.main.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.skin_model.main.button.create", "§aCreate");
        MESSAGES_EN.put("inGame.ui.skin_model.main.button.edit", "§6Edit");
        MESSAGES_EN.put("inGame.ui.skin_model.main.button.remove", "§cRemove");
        MESSAGES_EN.put("inGame.ui.skin_model.main.button.list", "§eList");
        MESSAGES_EN.put("inGame.ui.skin_model.edit_selection.title", "§6Select Model");
        MESSAGES_EN.put("inGame.ui.skin_model.edit_selection.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.skin_model.create.title", "§aCreate Model");
        MESSAGES_EN.put("inGame.ui.skin_model.create.element.id.text", "§7ID of the model");
        MESSAGES_EN.put("inGame.ui.skin_model.create.element.skin_file.text", "§7Texture path §8(§c./ §8= §eplugin_data folder§8)");
        MESSAGES_EN.put("inGame.ui.skin_model.create.element.geo_name.text", "§7Geometry identifier");
        MESSAGES_EN.put("inGame.ui.skin_model.create.element.geo_file.text", "§7Geometry path §8(§c./ §8= §eplugin_data folder§8)");
        MESSAGES_EN.put("inGame.ui.skin_model.remove.title", "§cRemove Model");
        MESSAGES_EN.put("inGame.ui.skin_model.remove.text", "§7Choose an option");
        MESSAGES_EN.put("inGame.ui.skin_model.edit.title", "§6Edit Model");
        MESSAGES_EN.put("inGame.ui.skin_model.edit.element.skin_file.text", "§7Texture path §8(§c./ §8= §eplugin_data folder§8)");
        MESSAGES_EN.put("inGame.ui.skin_model.edit.element.geo_name.text", "§7Geometry identifier");
        MESSAGES_EN.put("inGame.ui.skin_model.edit.element.geo_file.text", "§7Geometry path §8(§c./ §8= §eplugin_data folder§8)");
    }

    static {
        // raw
        MESSAGES_GER.put("raw.yes", "Ja");
        MESSAGES_GER.put("raw.no", "Nein");
        MESSAGES_GER.put("raw.author", "Autor(en)");
        MESSAGES_GER.put("raw.description", "Beschreibung");
        MESSAGES_GER.put("raw.enabled", "Aktiviert");

        // commands
        MESSAGES_GER.put("inGame.command.description.cloud", "Verwalte die Cloud");
        MESSAGES_GER.put("inGame.command.description.cloud_notify", "Aktiviere/Deaktiviere Benachrichtigungen");
        MESSAGES_GER.put("inGame.command.description.hub", "Gehe zur Lobby");
        MESSAGES_GER.put("inGame.command.description.cloudnpc", "Verwalte die Cloud-NPCs");
        MESSAGES_GER.put("inGame.command.description.template_group", "Verwalte die Template-Gruppen");
        MESSAGES_GER.put("inGame.command.description.transfer", "Verbinde dich mit anderen Servern");

        // general
        MESSAGES_GER.put("inGame.prefix", "§8[§l§3Pocket§bCloud§r§8] §r");
        MESSAGES_GER.put("inGame.no.permission", "{PREFIX}§cDafür hast du keine Rechte!");
        MESSAGES_GER.put("inGame.template.kick.maintenance", "§cDieses Template befindet sich momentan in Wartung.");

        // notify messages
        MESSAGES_GER.put("inGame.notify.message.server_starting", "{PREFIX}§fDer Server §b%server% §fwird §agestartet§r...");
        MESSAGES_GER.put("inGame.notify.message.server_stopping", "{PREFIX}§fDer Server §b%server% §fwird §cgestoppt§r...");
        MESSAGES_GER.put("inGame.notify.message.server_timed_out", "{PREFIX}§fDer Server §b%server% §fhat die Verbindung §cunterbrochen§f.");
        MESSAGES_GER.put("inGame.notify.message.server_stop_timed_out", "{PREFIX}§fDer Server §e%server% §fkonnte nicht gestoppt werden.");
        MESSAGES_GER.put("inGame.notify.message.server_crashed", "{PREFIX}§fDer Server §b%server% §ist §cabgestürzt!");
        MESSAGES_GER.put("inGame.notify.message.server_start_failed", "{PREFIX}§fDer Server §b%server% §rkonnte §cnicht §rgestartet werden: §b%reason%");
        MESSAGES_GER.put("inGame.notify.message.player_joined", "{PREFIX}§fSpieler §b%player% §fhat §averbunden §füber §b%server%§f.");
        MESSAGES_GER.put("inGame.notify.message.player_left", "{PREFIX}§fSpieler §b%player% §fhat die Verbindung zu §b%server% §cgetrennt§f.");
        MESSAGES_GER.put("inGame.notify.message.player_join_failed", "{PREFIX}§fSpieler §b%player% §fversuchte über §b%server%§f zu verbinden, §cscheiterte §fjedoch: §b%reason%");
        MESSAGES_GER.put("inGame.notify.message.player_kicked", "{PREFIX}§fSpieler §b%player% §fwurde von §b%server%§f §cgekickt§f: §b%reason%");
        MESSAGES_GER.put("inGame.notify.message.player_switched_server", "{PREFIX}§fSpieler §b%player% §fhat den Server gewechselt §8(§b%old_server% §8-> §b%new_server%§8)");

        // server
        MESSAGES_GER.put("inGame.server.verified", "§rDer Server wurde von der Cloud §averifiziert§r!");
        MESSAGES_GER.put("inGame.server.verify.denied", "§cVerifizierungsanfrage wurde von der Cloud abgelehnt! Stoppe Server...");
        MESSAGES_GER.put("inGame.server.verify.failed", "§cVerifizierungsanfrage wurde von der Cloud nicht bearbeitet! Stoppe Server...");
        MESSAGES_GER.put("inGame.network.receive.unknown", "§cUnbekanntes Paket von der Cloud erhalten!");
        MESSAGES_GER.put("inGame.server.timeout", "§cDie Cloud hat die Verbindung unterbrochen! Stoppe Server...");
        MESSAGES_GER.put("inGame.server.connect", "{PREFIX}§7Verbinde zu §e%0%§r...");
        MESSAGES_GER.put("inGame.server.already.connected", "{PREFIX}§cDu bist bereits mit §e%0% §cverbunden!");
        MESSAGES_GER.put("inGame.server.connect.failed", "{PREFIX}§cVerbindung zu §e%0% §ckonnte nicht hergestellt werden!");
        MESSAGES_GER.put("inGame.server.target.already.connected", "{PREFIX}§e%0% §cist bereits mit dem Server §e%1% §cverbunden!");
        MESSAGES_GER.put("inGame.server.target.connect", "{PREFIX}§e%0% §7verbindet mit §e%1%§7...");
        MESSAGES_GER.put("inGame.server.target.connect.failed", "{PREFIX}§e%0% §ckonnte nicht mit §e%1% §cverbunden werden!");
        MESSAGES_GER.put("inGame.server.not.found", "{PREFIX}§cServer wurde nicht gefunden!");
        MESSAGES_GER.put("inGame.server.saved", "{PREFIX}§aServer wurde gespeichert!");

        // misc not-found / state
        MESSAGES_GER.put("inGame.template.not.found", "{PREFIX}§cTemplate wurde nicht gefunden!");
        MESSAGES_GER.put("inGame.player.not.found", "{PREFIX}§cSpieler wurde nicht gefunden!");
        MESSAGES_GER.put("inGame.already.in.lobby", "{PREFIX}§cDu bist bereits in einer Lobby!");
        MESSAGES_GER.put("inGame.notify.activated", "{PREFIX}§aDu erhältst nun Benachrichtigungen von der §bCloud§a!");
        MESSAGES_GER.put("inGame.notify.deactivated", "{PREFIX}§cDu erhältst nun keine Benachrichtigungen mehr von der §bCloud§c!");
        MESSAGES_GER.put("inGame.max.servers.reached", "{PREFIX}§cMaximale Anzahl an Servern für das Template §e%0% §cerreicht!");

        // text actions
        MESSAGES_GER.put("inGame.text.successful.message", "{PREFIX}§aDu hast §e%0% §aerfolgreich eine §eNachricht §ageschickt!");
        MESSAGES_GER.put("inGame.text.successful.popup", "{PREFIX}§aDu hast §e%0% §aerfolgreich ein §ePopup §ageschickt!");
        MESSAGES_GER.put("inGame.text.successful.tip", "{PREFIX}§aDu hast §e%0% §aerfolgreich einen §eTipp §ageschickt!");
        MESSAGES_GER.put("inGame.text.successful.title", "{PREFIX}§aDu hast §e%0% §aerfolgreich einen §eTitel §ageschickt!");
        MESSAGES_GER.put("inGame.text.successful.action_bar", "{PREFIX}§aDu hast §e%0% §aerfolgreich eine §eActionbar-Nachricht §ageschickt!");
        MESSAGES_GER.put("inGame.text.successful.toast_notification", "{PREFIX}§aDu hast §e%0% §aerfolgreich eine §eToast-Benachrichtigung §ageschickt!");
        MESSAGES_GER.put("inGame.console.log.successful", "{PREFIX}§aText wurde erfolgreich an die §bCloud §ageschickt!");

        // modules
        MESSAGES_GER.put("inGame.module.enabled", "{PREFIX}§aDas Modul §e%0% §awurde erfolgreich aktiviert!");
        MESSAGES_GER.put("inGame.module.already.enabled", "{PREFIX}§cDas Modul §e%0% §cist bereits aktiviert!");
        MESSAGES_GER.put("inGame.module.disabled", "{PREFIX}§aDas Modul §e%0% §awurde erfolgreich deaktiviert!");
        MESSAGES_GER.put("inGame.module.already.disabled", "{PREFIX}§cDas Modul §e%0% §cist bereits deaktiviert!");
        MESSAGES_GER.put("inGame.module.no.enabled", "{PREFIX}§cEs sind keine Module aktiviert!");
        MESSAGES_GER.put("inGame.module.no.disabled", "{PREFIX}§cEs sind keine Module deaktiviert!");

        // cloudnpc
        MESSAGES_GER.put("inGame.cloudnpc.created", "{PREFIX}§aCloudNPC wurde erstellt!");
        MESSAGES_GER.put("inGame.cloudnpc.removed", "{PREFIX}§cCloudNPC wurde entfernt!");
        MESSAGES_GER.put("inGame.cloudnpc.select", "{PREFIX}§7Schlage einen CloudNPC, um ihn zu entfernen!");
        MESSAGES_GER.put("inGame.cloudnpc.process.cancelled", "{PREFIX}§cProzess wurde abgebrochen!");
        MESSAGES_GER.put("inGame.cloudnpc.quickjoin.no_server", "{PREFIX}§cKein Server gefunden!");
        MESSAGES_GER.put("inGame.cloudnpc.name_tag", "§e%1%\n§8» §7%0% spielen.");
        MESSAGES_GER.put("inGame.cloudnpc.name_tag.maintenance", "§e%1% §8- §c§lWARTUNG§r\n§8» §7%0% spielen.");

        // template groups
        MESSAGES_GER.put("inGame.template_group.exists", "{PREFIX}§cEine Template-Gruppe mit der Id §e%0% §cexistiert bereits!");
        MESSAGES_GER.put("inGame.template_group.created", "{PREFIX}§aDie Template-Gruppe mit der Id §e%0% §awurde erstellt!");
        MESSAGES_GER.put("inGame.template_group.removed", "{PREFIX}§cDie Template-Gruppe mit der Id §e%0% §cwurde entfernt!");

        // skin models
        MESSAGES_GER.put("inGame.skin_model.created", "{PREFIX}§aDas Model mit der Id §e%0% §awurde erstellt!");
        MESSAGES_GER.put("inGame.skin_model.edited", "{PREFIX}§aDas Model mit der Id §e%0% §awurde bearbeitet!");
        MESSAGES_GER.put("inGame.skin_model.removed", "{PREFIX}§cDas Model mit der Id §e%0% §cwurde entfernt!");
        MESSAGES_GER.put("inGame.skin_model.failed", "{PREFIX}§cDas Model mit der Id §e%0% §ckonnte nicht erstellt werden, da die Pfade nicht korrekt sind!");
        MESSAGES_GER.put("inGame.skin_model.exists", "{PREFIX}§cDas Model mit der Id §e%0% §cexistiert bereits!");

        // proxy
        MESSAGES_GER.put("inGame.proxy.stopped", "§f§lProxy wird heruntergefahren...");

        // ui - general
        MESSAGES_GER.put("inGame.ui.general.selection.title", "§lAuswahl");
        MESSAGES_GER.put("inGame.ui.general.selection.text", "§7Wähle eine Option aus");
        MESSAGES_GER.put("inGame.ui.general.selection.option.name", "§eAuswahl über Name");
        MESSAGES_GER.put("inGame.ui.general.selection.option.selection", "§eAuswahl über Dropdown");

        // ui - cloud main
        MESSAGES_GER.put("inGame.ui.cloud.main.title", "§3§lPocket§bCloud");
        MESSAGES_GER.put("inGame.ui.cloud.main.text", "§7Wähle eine Option aus");
        MESSAGES_GER.put("inGame.ui.cloud.main.button.manage_server", "§eVerwalte die Server");
        MESSAGES_GER.put("inGame.ui.cloud.main.button.manage_player", "§bVerwalte die Spieler");
        MESSAGES_GER.put("inGame.ui.cloud.main.button.manage_module", "§dVerwalte die Module");
        MESSAGES_GER.put("inGame.ui.cloud.main.button.manage_template", "§6Verwalte die Templates");
        MESSAGES_GER.put("inGame.ui.cloud.main.button.save_server", "§aSpeichere den Server");
        MESSAGES_GER.put("inGame.ui.cloud.main.button.cloud_log_console", "§fNachricht zur §bCloud §fKonsole");

        // ui - cloud log console
        MESSAGES_GER.put("inGame.ui.cloud_log_console.title", "§bCloud Konsole");
        MESSAGES_GER.put("inGame.ui.cloud_log_console.element.message.text", "§7Nachricht");
        MESSAGES_GER.put("inGame.ui.cloud_log_console.element.log_type.text", "§7Log-Typ");

        // ui - manage module
        MESSAGES_GER.put("inGame.ui.manage_module.title", "§dModule verwalten");
        MESSAGES_GER.put("inGame.ui.manage_module.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.manage_module.button.enable", "§aAktiviere ein Modul");
        MESSAGES_GER.put("inGame.ui.manage_module.button.disable", "§cDeaktiviere ein Modul");
        MESSAGES_GER.put("inGame.ui.manage_module.button.list", "§eListe alle Module");
        MESSAGES_GER.put("inGame.ui.manage_module.sub.enable.title", "§aModul aktivieren");
        MESSAGES_GER.put("inGame.ui.manage_module.sub.enable.name.text", "§7Modulname");
        MESSAGES_GER.put("inGame.ui.manage_module.sub.enable.dropdown.text", "§7Modul auswählen");
        MESSAGES_GER.put("inGame.ui.manage_module.sub.disable.title", "§cModul deaktivieren");
        MESSAGES_GER.put("inGame.ui.manage_module.sub.disable.name.text", "§7Modulname");
        MESSAGES_GER.put("inGame.ui.manage_module.sub.disable.dropdown.text", "§7Modul auswählen");

        // ui - manage player
        MESSAGES_GER.put("inGame.ui.manage_player.title", "§bSpieler verwalten");
        MESSAGES_GER.put("inGame.ui.manage_player.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.manage_player.button.text", "§fNachricht senden");
        MESSAGES_GER.put("inGame.ui.manage_player.button.kick", "§cSpieler kicken");
        MESSAGES_GER.put("inGame.ui.manage_player.button.list", "§eSpieler auflisten");
        MESSAGES_GER.put("inGame.ui.manage_player.button.info", "§bInfos über einen Spieler");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.text.title", "§fNachricht senden");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.text.name.text", "§7Spieler");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.text.message.text", "§7Nachricht");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.text.text_type.text", "§7Text-Typ");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.text.dropdown.text", "§7Spieler auswählen");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.kick.title", "§cSpieler kicken");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.kick.name.text", "§7Spieler");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.kick.reason.text", "§7Grund");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.kick.dropdown.text", "§7Spieler auswählen");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.info.title", "§bSpieler-Infos");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.info.name.text", "§7Spieler");
        MESSAGES_GER.put("inGame.ui.manage_player.sub.info.dropdown.text", "§7Spieler auswählen");

        // ui - manage server
        MESSAGES_GER.put("inGame.ui.manage_server.title", "§eServer verwalten");
        MESSAGES_GER.put("inGame.ui.manage_server.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.manage_server.button.start", "§aStarte Server");
        MESSAGES_GER.put("inGame.ui.manage_server.button.stop", "§cStoppe Server");
        MESSAGES_GER.put("inGame.ui.manage_server.button.list", "§eServer auflisten");
        MESSAGES_GER.put("inGame.ui.manage_server.button.info", "§bInfos über einen Server");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.start.title", "§aServer starten");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.start.name.text", "§7Template");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.start.count.text", "§7Anzahl an Servern");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.start.dropdown.text", "§7Template auswählen");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.stop.title", "§cServer stoppen");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.stop.name.text", "§7Servername");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.stop.dropdown.text", "§7Server auswählen");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.stop.template_option.text", "§7Soll das gesamte Template gestoppt werden?");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.stop.all_option.text", "§7Sollen alle Server gestoppt werden?");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.info.title", "§bServer-Infos");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.info.name.text", "§7Servername");
        MESSAGES_GER.put("inGame.ui.manage_server.sub.info.dropdown.text", "§7Server auswählen");

        // ui - manage template
        MESSAGES_GER.put("inGame.ui.manage_template.title", "§6Templates verwalten");
        MESSAGES_GER.put("inGame.ui.manage_template.button.info", "§bInfos über ein Template");
        MESSAGES_GER.put("inGame.ui.manage_template.button.list", "§eTemplates auflisten");
        MESSAGES_GER.put("inGame.ui.manage_template.sub.info.title", "§bTemplate-Infos");
        MESSAGES_GER.put("inGame.ui.manage_template.sub.info.dropdown.text", "§7Template auswählen");

        // kick
        MESSAGES_GER.put("inGame.kick.successful", "{PREFIX}§aDu hast §e%0% §aerfolgreich gekickt!");

        // ui - cloudnpc
        MESSAGES_GER.put("inGame.ui.cloudnpc.main.title", "§eNPCs verwalten");
        MESSAGES_GER.put("inGame.ui.cloudnpc.main.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.cloudnpc.main.button.create", "§aErstelle einen NPC");
        MESSAGES_GER.put("inGame.ui.cloudnpc.main.button.remove", "§cEntferne einen NPC");
        MESSAGES_GER.put("inGame.ui.cloudnpc.main.button.list", "§eListe alle NPCs");
        MESSAGES_GER.put("inGame.ui.cloudnpc.main.button.models", "§6Verwalte Skin-Modelle");
        MESSAGES_GER.put("inGame.ui.cloudnpc.create.title", "§aNPC erstellen");
        MESSAGES_GER.put("inGame.ui.cloudnpc.create.element.name.text", "§7Template");
        MESSAGES_GER.put("inGame.ui.cloudnpc.create.element.model.text", "§7Skin-Modell");
        MESSAGES_GER.put("inGame.ui.cloudnpc.create.element.headRotation.text", "§7Kopf-Rotation?");
        MESSAGES_GER.put("inGame.ui.cloudnpc.list.title", "§eNPC Liste");
        MESSAGES_GER.put("inGame.ui.cloudnpc.list.text", "§7Aktuell sind §e%0% NPC(s) §7verfügbar.");
        MESSAGES_GER.put("inGame.ui.cloudnpc.list_view.title", "§e%0%");
        MESSAGES_GER.put("inGame.ui.cloudnpc.list_view.button.teleport", "§aTeleportieren");
        MESSAGES_GER.put("inGame.ui.cloudnpc.list_view.button.back", "§cZurück");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_server.title", "§e%0%");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_server.text", "§e%0% Server §7mit dem Template §e%1% §7verfügbar.");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_server.no.server", "§cKeine Server verfügbar");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_server.button.server", "§e%0%\n§a%1%§8/§c%2%");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_template.title", "§e%0%");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_template.text", "§7Wähle ein Template aus der Gruppe §e%0% §7aus.");
        MESSAGES_GER.put("inGame.ui.cloudnpc.choose_template.button.template", "§e%0%\n§a%1%§8/§c%2%");

        // ui - template group
        MESSAGES_GER.put("inGame.ui.template_group.main.title", "§6Template-Gruppen verwalten");
        MESSAGES_GER.put("inGame.ui.template_group.main.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.template_group.main.button.create", "§aErstellen");
        MESSAGES_GER.put("inGame.ui.template_group.main.button.edit", "§6Bearbeiten");
        MESSAGES_GER.put("inGame.ui.template_group.main.button.remove", "§cEntfernen");
        MESSAGES_GER.put("inGame.ui.template_group.main.button.list", "§eListe");
        MESSAGES_GER.put("inGame.ui.template_group.create.title", "§aGruppe erstellen");
        MESSAGES_GER.put("inGame.ui.template_group.create.element.id.text", "§7Gruppen-ID");
        MESSAGES_GER.put("inGame.ui.template_group.create.element.display.text", "§7Anzeigename");
        MESSAGES_GER.put("inGame.ui.template_group.remove.title", "§cGruppe entfernen");
        MESSAGES_GER.put("inGame.ui.template_group.remove.text", "§7Wähle eine Template-Gruppe");
        MESSAGES_GER.put("inGame.ui.template_group.edit_selection.title", "§6Gruppe auswählen");
        MESSAGES_GER.put("inGame.ui.template_group.edit_selection.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.template_group.edit.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.template_group.edit.button.add_template", "§aTemplate hinzufügen");
        MESSAGES_GER.put("inGame.ui.template_group.edit.button.remove_template", "§cTemplate entfernen");
        MESSAGES_GER.put("inGame.ui.template_group.edit.button.change_display", "§fAnzeigename ändern");
        MESSAGES_GER.put("inGame.ui.template_group.add_template.title", "§aTemplate hinzufügen");
        MESSAGES_GER.put("inGame.ui.template_group.add_template.text", "§7Wähle ein Template");
        MESSAGES_GER.put("inGame.ui.template_group.remove_template.title", "§cTemplate entfernen");
        MESSAGES_GER.put("inGame.ui.template_group.remove_template.text", "§7Wähle ein Template");
        MESSAGES_GER.put("inGame.ui.template_group.change_display.title", "§fAnzeigename bearbeiten");
        MESSAGES_GER.put("inGame.ui.template_group.change_display.element.display", "§7Neuer Anzeigename");

        // ui - skin model
        MESSAGES_GER.put("inGame.ui.skin_model.main.title", "§6Skin-Modelle verwalten");
        MESSAGES_GER.put("inGame.ui.skin_model.main.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.skin_model.main.button.create", "§aErstellen");
        MESSAGES_GER.put("inGame.ui.skin_model.main.button.edit", "§6Bearbeiten");
        MESSAGES_GER.put("inGame.ui.skin_model.main.button.remove", "§cEntfernen");
        MESSAGES_GER.put("inGame.ui.skin_model.main.button.list", "§eListe");
        MESSAGES_GER.put("inGame.ui.skin_model.edit_selection.title", "§6Modell auswählen");
        MESSAGES_GER.put("inGame.ui.skin_model.edit_selection.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.skin_model.create.title", "§aModell erstellen");
        MESSAGES_GER.put("inGame.ui.skin_model.create.element.id.text", "§7ID des Modells");
        MESSAGES_GER.put("inGame.ui.skin_model.create.element.skin_file.text", "§7Textur-Dateipfad §8(§c./ §8= §eplugin_data Ordner§8)");
        MESSAGES_GER.put("inGame.ui.skin_model.create.element.geo_name.text", "§7Geometrie-Bezeichner");
        MESSAGES_GER.put("inGame.ui.skin_model.create.element.geo_file.text", "§7Geometrie-Dateipfad §8(§c./ §8= §eplugin_data Ordner§8)");
        MESSAGES_GER.put("inGame.ui.skin_model.remove.title", "§cModell entfernen");
        MESSAGES_GER.put("inGame.ui.skin_model.remove.text", "§7Wähle eine Option");
        MESSAGES_GER.put("inGame.ui.skin_model.edit.title", "§6Modell bearbeiten");
        MESSAGES_GER.put("inGame.ui.skin_model.edit.element.skin_file.text", "§7Textur-Dateipfad §8(§c./ §8= §eplugin_data Ordner§8)");
        MESSAGES_GER.put("inGame.ui.skin_model.edit.element.geo_name.text", "§7Geometrie-Bezeichner");
        MESSAGES_GER.put("inGame.ui.skin_model.edit.element.geo_file.text", "§7Geometrie-Dateipfad §8(§c./ §8= §eplugin_data Ordner§8)");
    }

    private DefaultMessages() {}
}