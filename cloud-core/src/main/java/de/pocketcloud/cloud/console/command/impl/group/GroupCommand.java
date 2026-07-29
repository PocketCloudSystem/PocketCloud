package de.pocketcloud.cloud.console.command.impl.group;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.builder.ServerGroupBuilder;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.*;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.template.group.ServerGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@CommandDescription(name = "group", description = "Manage the server groups")
public final class GroupCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("create", this::handleCreateSub, true, subCommand -> {
            subCommand.addParameter(new StringParameter("name", false))
                    .addParameter(new StringParameter("templates", true, true));
        }));

        registerSubCommand(SubCommand.lambda("edit", this::handleEditSub, true, subCommand -> {
            subCommand.addParameter(new ServerGroupParameter("group", false))
                    .addParameter(new StringEnumParameter("action", false, "add", "remove"))
                    .addParameter(new StringParameter("template", false, true));
        }));

        registerSubCommand(SubCommand.lambda("remove", this::handleRemoveSub, true, subCommand -> {
            subCommand.addParameter(new ServerGroupParameter("group", false));
        }));

        registerSubCommand(SubCommand.lambda("info", this::handleInfoSub, true, subCommand -> {
            subCommand.addParameter(new ServerGroupParameter("group", false));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true));
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    public boolean handleCreateSub(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("name");
        if (PocketCloud.instance().serverGroups().check(name)) {
            sender.warn("A server group with that name already exists.");
            return true;
        }

        String providedTemplates = ctx.argString("templates");
        List<ITemplate> templates = new ArrayList<>();
        if (providedTemplates != null) {
            for (String template : providedTemplates.split(" ")) {
                if (PocketCloud.instance().templates().check(template)) {
                    templates.add(PocketCloud.instance().templates().get(template).get());
                }
            }
        }

        PocketCloud.instance().serverGroups().create(ServerGroupBuilder.create().name(name).templates(templates.toArray(ITemplate[]::new)));
        return true;
    }

    public boolean handleEditSub(CommandSender sender, CommandContext ctx) {
        ServerGroup group = ctx.arg("group", ServerGroup.class);
        //TODO
        return true;
    }

    public boolean handleRemoveSub(CommandSender sender, CommandContext ctx) {
        ServerGroup group = ctx.arg("group", ServerGroup.class);
        PocketCloud.instance().serverGroups().remove(group);
        return true;
    }

    public boolean handleInfoSub(CommandSender sender, CommandContext ctx) {
        ServerGroup group = ctx.arg("group", ServerGroup.class);
        sender.info("Name§8: §b{}", group.name());
        sender.info("Templates§8: §b{}", String.join("§8, §b", group.templates()));
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        Collection<IServerGroup> groups = PocketCloud.instance().serverGroups().getAll();
        sender.info("ServerGroups §8(§b{}§8)§r:", groups.size());
        if (groups.isEmpty()) sender.info("§cNo server groups found.");
        for (IServerGroup group : groups) {
            sender.info("Name§8: §b{} §8| §rTemplates§8: §b{}", group.name(), String.join("§8, §b", group.templates()));
        }
        return true;
    }
}