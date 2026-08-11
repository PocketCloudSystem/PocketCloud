package de.pocketcloud.cloud.console.screen.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.command.sender.ConsoleCommandSender;
import de.pocketcloud.cloud.console.screen.Screen;
import de.pocketcloud.cloud.console.util.InterruptionResult;
import de.pocketcloud.common.util.FormatUtils;

public final class DefaultScreen extends Screen {

    @Override
    public void initialize(CloudConsole console) {
        console.resetPrompt();
        console.enableCompletion();
        prerenderStatus();
    }

    @Override
    public void tick(long currentTick) {
        if (currentTick % 10 == 0) {
            renderStatus();
        }
    }

    private void prerenderStatus() {
        showStatus(
                "",
                "  ",
                "   ",
                "    "
        );
    }

    private void renderStatus() {
        String tps = FormatUtils.tps(PocketCloud.instance().performanceStats().currentTPS());
        String avgTps = FormatUtils.tps(PocketCloud.instance().performanceStats().averageTPS());
        String memoryUsage = FormatUtils.bytes(PocketCloud.instance().performanceStats().processUsedMemory());
        String cpuUsage = FormatUtils.usagePercentage(PocketCloud.instance().performanceStats().processCpuUsage());
        int servers = PocketCloud.instance().servers().serverCount();
        int players = PocketCloud.instance().players().playerCount();

        String playerAndServerCount = "§8| §b" + players + " player" + (players == 1 ? "" : "s") + " §racross §b" + servers + " server" + (servers == 1 ? "" : "s");

        showStatus(
                "",
                "§8| §rTPS§8: " + tps + " §8(§rAvg. §b" + avgTps + "§8)",
                "§8| §rMemory Usage: §b" + memoryUsage + " §rand CPU Usage: " + cpuUsage,
                playerAndServerCount
        );
    }

    @Override
    public void onRemove(long currentTick) {
        hideStatus();
    }

    @Override
    public InterruptionResult onCancel(long currentTick) {
        PocketCloud.instance().commands().call(new ConsoleCommandSender(), "exit -y");
        return InterruptionResult.INTERRUPT;
    }

    @Override
    public void handleInput(String input) {
        PocketCloud.instance().commands().call(new ConsoleCommandSender(), input);
    }
}