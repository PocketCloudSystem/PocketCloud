package de.pocketcloud.cloud.console.command.impl.template;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.server.RollingRestart;
import de.pocketcloud.api.search.TemplateSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.sync.SyncUnit;
import de.pocketcloud.cloud.cluster.sync.TemplateReplicator;
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

import java.util.*;

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

        registerSubCommand(SubCommand.lambda("nodes", this::handleNodesSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false))
                    .addParameter(new StringParameter("nodes", true));
        }));

        registerSubCommand(SubCommand.lambda("staticnode", this::handleStaticNodeSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false))
                    .addParameter(new StringParameter("node", true));
        }));

        registerSubCommand(SubCommand.lambda("info", this::handleInfoSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false));
        }));

        registerSubCommand(SubCommand.lambda("list", this::handleListSub, true, subCommand -> {
            subCommand.addParameter(new MultipleTypesParameter("filter", true, new TemplateTypeParameter("template_type", true),
                    new ServerGroupParameter("server_group", true),
                    new SoftwareParameter("software", true)));
        }));

        registerSubCommand(SubCommand.lambda("restart", this::handleRestartSub, true, subCommand -> {
            subCommand.addParameter(new TemplateParameter("template", false));
        }));

        registerSubCommand(SubCommand.lambda("history", this::handleHistorySub, true, subCommand -> {
            subCommand.addParameter(new StringParameter("template", false));
        }));

        registerSubCommand(SubCommand.lambda("rollback", this::handleRollbackSub, true, subCommand -> {
            subCommand.addParameter(new StringParameter("template", false))
                    .addParameter(new IntegerParameter("version", false));
        }));

        registerSubCommand(SubCommand.lambda("sync", this::handleSyncSub, true, subCommand -> {
            subCommand.addParameter(new StringEnumParameter("action", false, "pause", "resume", "status"))
                    .addParameter(new StringParameter("template", true));
        }));
    }

    private static Optional<TemplateReplicator> replicator(CommandSender sender) {
        Optional<TemplateReplicator> replicator = TemplateReplicator.get();
        if (replicator.isEmpty()) sender.error("Template replication only runs while this node is part of a cluster.");
        return replicator;
    }

    public boolean handleRestartSub(CommandSender sender, CommandContext ctx) {
        ITemplate template = ctx.arg("template", ITemplate.class);
        if (!RollingRestart.start(template, sender::info)) sender.error("The servers of §b{} §rare already being restarted.", template.name());
        return true;
    }

    public boolean handleHistorySub(CommandSender sender, CommandContext ctx) {
        SyncUnit unit = SyncUnit.parse(ctx.argString("template"));
        replicator(sender).ifPresent(r -> r.history(unit, lines -> lines.forEach(sender::info)));
        return true;
    }

    public boolean handleRollbackSub(CommandSender sender, CommandContext ctx) {
        SyncUnit unit = SyncUnit.parse(ctx.argString("template"));
        int version = ctx.argInt("version");
        replicator(sender).ifPresent(r -> r.rollback(unit, version, lines -> lines.forEach(sender::info)));
        return true;
    }

    public boolean handleSyncSub(CommandSender sender, CommandContext ctx) {
        String action = ctx.argString("action").toLowerCase();
        String name = ctx.hasArg("template") ? ctx.argString("template") : null;
        SyncUnit unit = name == null ? null : SyncUnit.parse(name);
        replicator(sender).ifPresent(r -> {
            switch (action) {
                case "pause" -> {
                    r.pause(unit);
                    sender.success("Template sync §cpaused §rfor §b{}§r. Local changes are neither sent nor overwritten until §btemplate sync resume§r.", unit == null ? "all templates" : unit.display());
                }
                case "resume" -> {
                    r.resume(unit);
                    sender.success("Template sync §aresumed §rfor §b{}§r.", unit == null ? "all templates" : unit.display());
                }
                default -> r.statusLines().forEach(sender::info);
            }
        });
        return true;
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
            PocketCloud.instance().templates().delete(template);
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

    /**
     * {@code template nodes <template>} shows, {@code template nodes <template> <node,node|all>} sets the nodes
     * servers of the template may run on.
     */
    public boolean handleNodesSub(CommandSender sender, CommandContext ctx) {
        Template template = ctx.arg("template", Template.class);
        if (!ctx.args().containsKey("nodes")) {
            sender.info("Nodes of §b{}§8: §b{}", template.name(), formatNodes(template.settings().nodes()));
            return true;
        }

        String raw = ctx.argString("nodes").trim();
        List<String> nodes = raw.equalsIgnoreCase("all") || raw.equals("-") || raw.equals("[]")
                ? List.of()
                : Arrays.stream(raw.replace("[", "").replace("]", "").split(","))
                .map(String::trim).filter(n -> !n.isEmpty()).distinct().toList();

        for (String node : nodes) {
            if (PocketCloud.instance().cluster().node(node).isEmpty()) {
                sender.warn("The node §b{} §ris not a member of this cluster (yet).", node);
            }
        }

        PocketCloud.instance().templates().edit(template, TemplateEditData.create().nodes(nodes));
        sender.success("Servers of §b{} §rnow run on§8: §b{}", template.name(), formatNodes(nodes));
        return true;
    }

    /**
     * {@code template staticnode <template>} shows, {@code template staticnode <template> <node|none>} sets the node
     * the static servers of the template live on (required for static templates in a cluster).
     */
    public boolean handleStaticNodeSub(CommandSender sender, CommandContext ctx) {
        Template template = ctx.arg("template", Template.class);
        if (!ctx.args().containsKey("node")) {
            sender.info("Static node of §b{}§8: §b{}", template.name(), formatStaticNode(template));
            return true;
        }

        String raw = ctx.argString("node").trim();
        String node = raw.equalsIgnoreCase("none") || raw.equals("-") ? "" : raw;
        if (!node.isEmpty() && PocketCloud.instance().cluster().node(node).isEmpty()) {
            sender.warn("The node §b{} §ris not a member of this cluster (yet).", node);
        }
        if (!node.isEmpty() && !template.settings().allowsNode(node)) {
            sender.warn("The node §b{} §ris not in the nodes of §b{}§r: its servers will not start until you add it.", node, template.name());
        }

        PocketCloud.instance().templates().edit(template, TemplateEditData.create().staticNode(node));
        if (node.isEmpty()) sender.success("Removed the static node of §b{}§r.", template.name());
        else sender.success("Static servers of §b{} §rnow live on node §b{}§r.", template.name(), node);
        if (!template.settings().staticServers()) sender.info("§7(The template has no static servers; the setting only matters once it has.)");
        return true;
    }

    private static String formatStaticNode(ITemplate template) {
        if (template.settings().hasStaticNode()) return template.settings().staticNode();
        boolean required = template.settings().staticServers() && PocketCloud.instance().cluster().clustered();
        return required ? "§cnot set §8(§rrequired: no static server starts§8)" : "§7not set";
    }

    private static String formatNodes(List<String> nodes) {
        return nodes == null || nodes.isEmpty() ? "all nodes" : String.join("§8, §b", nodes);
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
        sender.info("startNewServerThreshold§8: §b{}%", template.settings().startNewServerThreshold() * 100);
        sender.info("AutoStart§8: §b{}", template.settings().autoStart() ? "§aYes" : "§cNo");
        sender.info("Nodes§8: §b{}", formatNodes(template.settings().nodes()));
        sender.info("StaticNode§8: §b{}", formatStaticNode(template));
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
        sender.info("Templates §8(§b{}§8/§b{}§8)§r:", templates.size(), PocketCloud.instance().templates().templateCount());
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
        if (args.size() == 3) {
            return TemplateHelper.EDITABLE_KEYS;
        } else if (args.size() == 4) {
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