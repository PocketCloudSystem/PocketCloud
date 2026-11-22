package de.pocketcloud.cloud.console.command.impl.node;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterMembers;
import de.pocketcloud.cloud.cluster.HeadElection;
import de.pocketcloud.cloud.cluster.NodeSelector;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.scheduling.SchedulingModule;
import de.pocketcloud.cloud.console.command.flag.CommandFlag;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.TabComplete;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.parameter.def.IntegerParameter;
import de.pocketcloud.cloud.console.command.parameter.def.StringParameter;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.cloud.console.command.sub.SubCommand;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@CommandDescription(name = "node", description = "Manage the nodes of the cluster", aliases = {"nodes", "cluster"})
public final class NodeCommand extends Command implements TabComplete {

    @Override
    public void prepare() {
        registerSubCommand(SubCommand.lambda("list", this::handleList, true));
        registerSubCommand(SubCommand.lambda("info", this::handleInfo, true, sub ->
                sub.addParameter(new StringParameter("node", false))));
        registerSubCommand(SubCommand.lambda("add", this::handleAdd, true, sub ->
                sub.addParameter(new StringParameter("node", false))
                        .addParameter(new StringParameter("address", false))
                        .addParameter(new IntegerParameter("port", true))));
        registerSubCommand(SubCommand.lambda("remove", this::handleRemove, true, sub ->
                sub.addParameter(new StringParameter("node", false))));
        registerSubCommand(SubCommand.lambda("drain", (sender, ctx) -> handleDrain(sender, ctx, true), true, sub ->
                sub.addParameter(new StringParameter("node", false))
                        .addFlag(CommandFlag.longFlag("stop"))));
        registerSubCommand(SubCommand.lambda("undrain", (sender, ctx) -> handleDrain(sender, ctx, false), true, sub ->
                sub.addParameter(new StringParameter("node", false))));
    }

    /**
     * {@code node drain <node> [--stop]}: the node gets no new servers; running servers keep running unless
     * {@code --stop} is given (then its non-static servers are stopped and replaced on other nodes).
     * {@code node undrain <node>} ends it. Works on every node, the change is made by the head.
     */
    private boolean handleDrain(CommandSender sender, CommandContext ctx, boolean drain) {
        String name = ctx.argString("node");
        SchedulingModule module = SchedulingModule.get().orElse(null);
        if (module == null) {
            sender.error("Drain mode only exists in a cluster.");
            return true;
        }
        module.setDrain(name, drain, drain && ctx.hasFlag("stop"))
                .thenSuccess(sender::success)
                .failure(e -> sender.error("Could not {} §b{}§r: {}", drain ? "drain" : "undrain", name, e.getMessage()));
        return true;
    }

    private static String drainTag(String node) {
        return SchedulingModule.get().map(m -> m.draining(node)).orElse(false) ? " §e[DRAIN]" : "";
    }

    private static String cpu(Object value) {
        if (!(value instanceof Number n)) return "§7-";
        double cpu = n.doubleValue();
        return (cpu >= NodeSelector.CPU_LIMIT ? "§c" : "§a") + String.format(java.util.Locale.ROOT, "%.0f%%", cpu);
    }

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        return true;
    }

    private boolean handleList(CommandSender sender, CommandContext ctx) {
        ClusterManager cluster = PocketCloud.instance().cluster();
        HeadElection election = cluster.election();
        if (!cluster.clustered()) {
            sender.info("This node (§b{}§r) runs §balone §rand is always head. Use §bnode add <name> <address> §rto create a cluster.", cluster.nodeName());
            return true;
        }

        long now = System.currentTimeMillis();
        List<ClusterMembers.Member> members = cluster.members().all();
        sender.info("Nodes §8(§b{}§8) §8| §rhead§8: §b{} §8| §rterm§8: §b{} §8| §rmajority§8: §b{}",
                members.size(), election.head() == null ? "§cnone" : election.head(), election.term(), cluster.quorum());
        for (ClusterMembers.Member member : members) {
            boolean self = member.name().equals(cluster.nodeName());
            String head = (member.name().equals(election.head()) ? " §6[HEAD]" : "") + drainTag(member.name());
            if (self) {
                sender.info("§b{}{} §8(§rthis node§8) §8| §r{}:{} §8| §rstate§8: §b{} §8| §rcpu§8: {}", member.name(), head, member.address(), member.port(), election.state(),
                        cpu(PocketCloud.instance().performanceStats().systemCpuUsage()));
                continue;
            }

            Peer peer = cluster.peer(member.name()).orElse(null);
            String health = peer == null ? "§cDEAD" : color(peer.health(now));
            String seen = peer == null || peer.lastSeen() == 0 ? "never" : ((now - peer.lastSeen()) / 1000) + "s ago";
            String state = peer == null ? "-" : peer.state();
            sender.info("§b{}{} §8| §r{}:{} §8| §r{} §8| §rstate§8: §b{} §8| §rcpu§8: {} §8| §rlast seen§8: §b{}", member.name(), head, member.address(), member.port(), health, state,
                    cpu(peer == null ? null : peer.stats().get("cpu")), seen);
        }
        return true;
    }

    private boolean handleInfo(CommandSender sender, CommandContext ctx) {
        ClusterManager cluster = PocketCloud.instance().cluster();
        String name = ctx.argString("node");
        ClusterMembers.Member member = cluster.members().get(name).orElse(null);
        if (member == null) {
            sender.error("The node §b{} §rdoes not exist.", name);
            return true;
        }

        sender.info("Name§8: §b{}", member.name());
        sender.info("Address§8: §b{}:{}", member.address(), member.port());
        SchedulingModule scheduling = SchedulingModule.get().orElse(null);
        if (scheduling != null) {
            sender.info("Drain§8: {}", scheduling.draining(name) ? "§eYes §8(§rno new servers§8)" : "§aNo");
            Collection<String> softwares = scheduling.softwaresOf(name);
            sender.info("Software§8: §b{}", softwares == null ? "§7unknown" : softwares.isEmpty() ? "§cnone" : String.join("§8, §b", softwares));
        }
        if (member.name().equals(cluster.nodeName())) {
            sender.info("State§8: §b{} §8(§rthis node§8)", cluster.election().state());
            sender.info("Running since§8: §b{}", format(System.currentTimeMillis() - cluster.election().startedAt()));
            cpuLine(sender, PocketCloud.instance().performanceStats().systemCpuUsage());
            return true;
        }

        cluster.peer(name).ifPresentOrElse(peer -> {
            long now = System.currentTimeMillis();
            sender.info("Health§8: {}", color(peer.health(now)));
            sender.info("State§8: §b{}", peer.state());
            sender.info("Connected§8: §b{} §8({})", peer.connected(), peer.outbound() ? "outgoing" : "incoming");
            sender.info("Term§8: §b{}", peer.term());
            if (peer.identity() != null) {
                sender.info("Version§8: §b{}", peer.identity().version());
                sender.info("Running since§8: §b{}", format(now - peer.identity().startedAt()));
            }
            Map<String, Object> stats = peer.stats();
            if (!stats.isEmpty()) sender.info("Servers§8: §b{} §8| §rCloud memory§8: §b{} MB", stats.get("servers"), stats.get("usedMemoryMb"));
            cpuLine(sender, stats.get("cpu"));
        }, () -> sender.info("Health§8: §cDEAD §8(§rnever connected§8)"));
        return true;
    }

    private static void cpuLine(CommandSender sender, Object value) {
        boolean busy = value instanceof Number n && n.doubleValue() >= NodeSelector.CPU_LIMIT;
        sender.info("CPU§8: {} {}", cpu(value), busy
                ? "§8(§cover the limit of " + (int) NodeSelector.CPU_LIMIT + "%§r: no new servers right now§8)"
                : "§8(§rlimit for new servers§8: §b" + (int) NodeSelector.CPU_LIMIT + "%§8)");
    }

    private boolean handleAdd(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("node");
        String address = ctx.argString("address");
        int port = ctx.args().containsKey("port") ? ctx.argInt("port") : 3657;
        try {
            String token = PocketCloud.instance().cluster().addNode(name, address, port);
            sender.success("Node §b{} §radded. Start it once with this token (it contains a private key, keep it secret):", name);
            sender.info("java -jar pocketcloud.jar --join {}", token);
            int members = PocketCloud.instance().cluster().members().size();
            if (members == 2) {
                sender.warn("With 2 nodes the majority is 2: until §b{} §rhas joined, there is no head and no new servers are started.", name);
            }
        } catch (Exception e) {
            sender.error("Could not add node §b{}§r: {}", name, e.getMessage());
        }
        return true;
    }

    private boolean handleRemove(CommandSender sender, CommandContext ctx) {
        String name = ctx.argString("node");
        try {
            if (PocketCloud.instance().cluster().removeNode(name)) sender.success("Node §b{} §rremoved. It can no longer connect.", name);
            else sender.error("The node §b{} §rdoes not exist.", name);
        } catch (Exception e) {
            sender.error("Could not remove node §b{}§r: {}", name, e.getMessage());
        }
        return true;
    }

    private static String color(String health) {
        return switch (health) {
            case "ALIVE" -> "§aALIVE";
            case "SUSPECT" -> "§eSUSPECT";
            default -> "§cDEAD";
        };
    }

    private static String format(long ms) {
        Duration d = Duration.ofMillis(ms);
        return d.toHoursPart() + d.toDaysPart() * 24 + "h " + d.toMinutesPart() + "m " + d.toSecondsPart() + "s";
    }

    @Override
    public Collection<String> onTabComplete(List<String> args) {
        if (args.size() == 2 && List.of("info", "remove", "drain", "undrain").contains(args.getFirst())) {
            return PocketCloud.instance().cluster().members().all().stream().map(ClusterMembers.Member::name).toList();
        }
        return List.of();
    }
}
