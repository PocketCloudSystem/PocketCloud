package de.pocketcloud.cloud.console.command.impl.template;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.TemplateSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.TabComplete;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.parameter.def.*;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.setup.def.TemplateCreationSetup;
import de.pocketcloud.cloud.setup.def.TemplateEditSetup;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.util.TemplateHelper;
import de.pocketcloud.shared.component.software.ServerSoftware;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@CommandDescription(name = "template", description = "Manage the templates", aliases = {"temp"})
public final class TemplateCommand extends Command implements TabComplete {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("create", this::handleCreateSub, true));

        registerSubCommand(SubCommand.lambda("remove", this::handleRemoveSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false))
                    .addFlag(CommandFlag.shortFlag("y"));
        }));

        registerSubCommand(SubCommand.lambda("edit", this::handleEditSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false));
        }));

        registerSubCommand(SubCommand.lambda("info", this::handleInfoSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true, subCommand -> {
            subCommand.addParameter(new MultipleTypesParameter("filter", true, new TemplateTypeParameter("template_type", true),
                    new ServerGroupParameter("server_group", true),
                    new SoftwareParameter("software", true)));
        }));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    public boolean handleCreateSub(CommandSender sender, CommandContext ctx) {
        new TemplateCreationSetup().startSetup();
        return true;
    }

    public boolean handleRemoveSub(CommandSender sender, CommandContext ctx) {
        Template template = ctx.arg("template", Template.class);
        boolean sure = ctx.hasFlag("y");
        if (sure) {
            PocketCloud.instance().templates().remove(template);
        } else {
            awaitConfirmation(sender, "Are you sure you want to §cremove §rthe template §b" + template.name() + "§r?")
                    .thenSuccess(r -> {
                        if (r) {
                            PocketCloud.instance().templates().delete(template);
                        }
                    });
        }
        return true;
    }

    public boolean handleEditSub(CommandSender sender, CommandContext ctx) {
        Template template = ctx.arg("template", Template.class);
        new TemplateEditSetup(template).startSetup();
        return true;
    }

    public boolean handleInfoSub(CommandSender sender, CommandContext ctx) {
        Template template = ctx.arg("template", Template.class);
        sender.info("Name§8: §b{}", template.name());
        sender.info("Lobby§8: §b{}", template.settings().lobby() ? "§aYes" : "§cNo");
        sender.info("Maintenance§8: §b{}", template.settings().maintenance() ? "§aYes" : "§cNo");
        sender.info("Static§8: §b{}", template.settings().staticServers() ? "§aYes" : "§cNo");
        sender.info("AlwaysCopyToStaticServers§8: §b{}", template.settings().alwaysCopyToStaticServers() ? "§aYes" : "§cNo");
        sender.info("SaveOnShutdown§8: §b{}", template.settings().saveOnShutdown() ? "§aYes" : "§cNo");
        sender.info("MaxPlayerCount§8: §b{} player(s)", template.settings().maxPlayerCount());
        sender.info("MinServerCount§8: §b{} server(s)", template.settings().minServerCount());
        sender.info("MaxServerCount§8: §b{} server(s)", template.settings().maxServerCount());
        sender.info("StartNewPercentage§8: §b{}%", template.settings().startNewPercentage() * 100);
        sender.info("AutoStart§8: §b{}", template.settings().autoStart() ? "§aYes" : "§cNo");
        sender.info("Software§8: §b{} §8(§b{}§8)", template.serverSoftware().name(), template.templateType().name());
        sender.info("§b{} player(s) §racross §b{} server(s)§r.", template.playerCount(), template.serverCount());
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        TemplateSearchQuery query = TemplateSearchQuery.create();
        Object filter = ctx.arg("filter");
        if (filter instanceof TemplateType type) {
            query.ofTemplateType(type);
        } else if (filter instanceof ServerGroup group) {
            query.ofServerGroup(group);
        } else if (filter instanceof ServerSoftware software) {
            query.runningSoftware(software.name());
        }

        Collection<ITemplate> templates = PocketCloud.instance().templates().query(query).stream().sorted(Comparator.comparingInt(ITemplate::playerCount)).toList();
        sender.info("Templates §8(§b{}§8/§b{}§8)§r:", templates.size(), PocketCloud.instance().templates().getAll().size());
        if (templates.isEmpty()) sender.info("§cNo templates found.");
        for (ITemplate template : templates) {
            sender.info("Name§8: §b{} §8| §rPlayers§8: §b{}§8/§c{} §8| §rServers§8: §b{}§8/§c{} §8| §rLobby§8: §b{} §8| §rMaintenance§8: §b{} §8| §rSoftware§8: §b{} §8(§b{}§8)",
                    template.name(), template.playerCount(), template.settings().maxPlayerCount(), template.serverCount(), template.settings().maxServerCount(),
                    template.settings().lobby() ? "§aYes" : "§cNo", template.settings().maintenance() ? "§aYes" : "§cNo",
                    template.serverSoftware().name(), template.templateType().name());
        }

        return true;
    }

    @Override
    public Collection<String> onTabComplete(List<String> args) {
        if (args.size() == 4) {
            // template (/) edit (0) Lobby-1 (1) autoStart (2) false (3)
            if (args.getFirst().equals("edit")) {
                String key = TemplateHelper.convert(args.get(2));
                if (TemplateHelper.checkKey(key) && TemplateHelper.getKeyType(key) == Boolean.class) {
                    return List.of("true", "false");
                }
            }
        }
        return List.of();
    }
}