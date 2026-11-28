package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.StringParameter;
import de.pocketcloud.cloud.console.command.parameter.def.SuggestingParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.panel.PanelSessions;
import de.pocketcloud.cloud.http.panel.PanelUsers;

import java.util.List;

/** Web panel users. Passwords are generated here (nothing secret is typed into the console history). */
@CommandDescription(name = "user", description = "Manage the users of the web panel", aliases = {"users"})
public final class UserCommand extends Command {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("create", this::handleCreateSub, true, sub -> sub
                .addParameter(new StringParameter("name", false))
                .addParameter(role())));
        registerSubCommand(SubCommand.lambda("role", this::handleRoleSub, true, sub -> sub
                .addParameter(user())
                .addParameter(role())));
        registerSubCommand(SubCommand.lambda("password", this::handlePasswordSub, true, sub -> sub.addParameter(user())));
        registerSubCommand(SubCommand.lambda("remove", this::handleRemoveSub, true, sub -> sub.addParameter(user())));
        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true));
    }

    private static SuggestingParameter user() {
        return new SuggestingParameter("name", false, () -> PanelUsers.list().stream().map(PanelUsers.User::name).toList());
    }

    private static SuggestingParameter role() {
        return new SuggestingParameter("role", false, () -> List.copyOf(PanelUsers.ROLES.keySet()));
    }

    public boolean handleCreateSub(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("name");
        if (PanelUsers.get(name).isPresent()) {
            sender.error("§cThe user §b{} §calready exists. Use §buser role §cor §buser password§c.", name);
            return true;
        }
        String password = PanelUsers.randomPassword();
        if (save(sender, name, password, ctx.argString("role"))) {
            sender.success("Created the panel user §b{} §rwith the role §b{}§r.", name, ctx.argString("role"));
            sender.info("Password§8: §e{} §8(§rchange it in the panel under your name§8)", password);
        }
        return true;
    }

    public boolean handleRoleSub(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("name");
        if (exists(sender, name) && save(sender, name, null, ctx.argString("role"))) {
            PanelSessions.removeUser(name);
            sender.success("§b{} §rnow has the role §b{}§r.", name, ctx.argString("role"));
        }
        return true;
    }

    public boolean handlePasswordSub(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("name");
        String password = PanelUsers.randomPassword();
        if (exists(sender, name) && save(sender, name, password, null)) {
            PanelSessions.removeUser(name);
            sender.success("New password for §b{}§8: §e{}", name, password);
        }
        return true;
    }

    public boolean handleRemoveSub(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("name");
        if (PanelUsers.remove(name)) {
            PanelSessions.removeUser(name);
            sender.success("Removed the panel user §b{}§r.", name);
        } else sender.error("§cThere is no panel user §b{}§c.", name);
        return true;
    }

    public boolean handleListSub(CommandSender sender, CommandContext ctx) {
        List<PanelUsers.User> users = PanelUsers.list();
        if (users.isEmpty()) {
            sender.info("There are no panel users. Create one with §buser create <name> admin§r.");
            return true;
        }
        sender.info("Panel users §8(§b{}§8)§r:", users.size());
        for (PanelUsers.User u : users) sender.info("§b{} §8| §r{}", u.name(), u.role());
        sender.info("Roles§8: §r{}", String.join(", ", PanelUsers.ROLES.keySet()));
        return true;
    }

    private static boolean exists(CommandSender sender, String name) {
        if (PanelUsers.get(name).filter(u -> !u.name().equals(PanelUsers.KEY_USER)).isPresent()) return true;
        sender.error("§cThere is no panel user §b{}§c.", name);
        return false;
    }

    private static boolean save(CommandSender sender, String name, String password, String role) {
        try {
            PanelUsers.save(name, password, role);
            return true;
        } catch (HttpException e) {
            sender.error("§c{}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return false;
    }
}
