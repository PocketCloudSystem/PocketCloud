package de.pocketcloud.cloud.console.screen.impl;

import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.CloudConsole;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.output.OutputManager;
import de.pocketcloud.cloud.console.output.impl.ServerConsoleOutputHandler;
import de.pocketcloud.cloud.console.screen.Screen;
import de.pocketcloud.cloud.console.util.InterruptionResult;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.util.ServerLogStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ServerConsoleMonitorScreen extends Screen {

    private static final int[][] MC_RGB = {
            {0,0,0}, {0,0,170}, {0,170,0}, {0,170,170}, {170,0,0}, {170,0,170}, {255,170,0}, {170,170,170},
            {85,85,85}, {85,85,255}, {85,255,85}, {85,255,255}, {255,85,85}, {255,85,255}, {255,255,85}, {255,255,255}
    };

    private static final String[] MC_CODES = {"§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7", "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"};

    private static final Pattern CSI_SEQUENCE = Pattern.compile("\u001B\\[[0-?]*[ -/]*[@-~]");
    private static final Pattern SGR_PARAMS = Pattern.compile("^\u001B\\[([0-9;]*)m$");

    private ILogger logger;
    private ServerLogStream stream;

    private boolean justStopped = false;
    private String lastInfoMessage = "";
    private long nextOpenStreamTry = 0;

    private final String serverName;

    public ServerConsoleMonitorScreen(String serverName) {
        this.serverName = serverName;
    }

    private String processAnsi(String line) {
        Matcher matcher = CSI_SEQUENCE.matcher(line);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String seq = matcher.group();
            Matcher sgr = SGR_PARAMS.matcher(seq);
            String replacement = sgr.matches() ? translateSgr(sgr.group(1)) : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private String translateSgr(String params) {
        if (params.isEmpty()) return "§r";

        String[] tokens = params.split(";", -1);
        String color = null;
        StringBuilder formats = new StringBuilder();
        boolean reset = false;

        for (int i = 0; i < tokens.length; i++) {
            switch (tokens[i]) {
                case "0" -> { reset = true; color = null; formats.setLength(0); }
                case "1" -> formats.append("§l");
                case "3" -> formats.append("§o");
                case "4" -> formats.append("§n");
                case "9" -> formats.append("§m");
                case "30" -> color = "§0";
                case "31", "91" -> color = "§c";
                case "32", "92" -> color = "§a";
                case "33", "93" -> color = "§e";
                case "34", "94" -> color = "§9";
                case "35", "95" -> color = "§d";
                case "36", "96" -> color = "§b";
                case "37", "97" -> color = "§f";
                case "90" -> color = "§8";
                case "39" -> color = "§r";
                case "38" -> {
                    if (i + 2 < tokens.length && tokens[i + 1].equals("5")) {
                        color = nearestMcColor256(parseIntSafe(tokens[i + 2]));
                        i += 2;
                    } else if (i + 4 < tokens.length && tokens[i + 1].equals("2")) {
                        color = nearestMcColorRgb(parseIntSafe(tokens[i + 2]), parseIntSafe(tokens[i + 3]), parseIntSafe(tokens[i + 4]));
                        i += 4;
                    }
                }
                default -> {}
            }
        }

        if (color == null && formats.isEmpty()) return reset ? "§r" : "";
        return (color != null ? color : "") + formats;
    }

    private String nearestMcColor256(int idx) {
        int r, g, b;
        if (idx < 16) {
            return MC_CODES[idx];
        } else if (idx < 232) {
            int n = idx - 16;
            int[] levels = {0, 95, 135, 175, 215, 255};
            r = levels[(n / 36) % 6];
            g = levels[(n / 6) % 6];
            b = levels[n % 6];
        } else {
            r = g = b = 8 + (idx - 232) * 10;
        }
        return nearestMcColorRgb(r, g, b);
    }

    private String nearestMcColorRgb(int r, int g, int b) {
        int bestIdx = 0, bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < MC_RGB.length; i++) {
            int dr = r - MC_RGB[i][0], dg = g - MC_RGB[i][1], db = b - MC_RGB[i][2];
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) { bestDist = dist; bestIdx = i; }
        }
        return MC_CODES[bestIdx];
    }

    private int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void openLogStream() {
        if (stream != null) stream.stopStream();
        CloudServer server = (CloudServer) PocketCloud.instance().servers().get(serverName).orElse(null);
        if (server != null) {
            stream = server.openLogStream();
            try {
                stream.startStream();
            } catch (Exception e) {
                printInfoMessage(
                    "§8[§c!§8] §cFailed to open log stream§8: §e{}§r, trying again in 3 seconds...",
                    e.getMessage()
                );

                nextOpenStreamTry = PocketCloud.instance().currentTick() + (20 * 3);
                stream = null;
            }
        } else {
            printInfoMessage(
                "§8[§c!§8] §rThe server §b{} §rwas not found. Press §bCTRL + C §rto §ccancel§r.",
                serverName
            );
        }
    }

    private void checkLogStream() {
        CloudServer server = (CloudServer) PocketCloud.instance().servers().get(serverName).orElse(null);
        if (server == null) {
            if (stream == null) {
                if (!justStopped) {
                    printInfoMessage(
                        "§8[§c!§8] §rThe server §b{} §rwas not found. Press §bCTRL + C §rto §ccancel§r.",
                        serverName
                    );
                }
            } else {
                printInfoMessage(
                    "§8[§c!§8] §rThe server §b{} §rhas been stopped. Press §bCTRL + C §rto §ccancel §ror continue waiting.",
                    serverName
                );

                stream.stopStream();
                stream = null;
                justStopped = true;
            }
        } else {
            if (stream == null && PocketCloud.instance().currentTick() >= nextOpenStreamTry) {
                justStopped = false;
                printInfoMessage(
                    "§8[§c!§8] §rThe server §b{} §rhas been §astarted§r. Starting log stream...",
                    serverName
                );
                openLogStream();
            }
        }
    }

    @Override
    public void initialize(CloudConsole console) {
        clear();
        enableCompletion();

        logger = CloudLogger.tmp();

        showStatus("§rCurrently streaming §b" + serverName);

        ServerConsoleOutputHandler outputHandler = new ServerConsoleOutputHandler();
        OutputManager.set(outputHandler);

        outputHandler.add(logger);

        openLogStream();
    }

    @Override
    public void handleInput(String input) {
        if (input.trim().isEmpty()) return;
        CloudServer server = (CloudServer) PocketCloud.instance().servers().get(serverName).orElse(null);
        if (server != null) {
            server.dispatch(input);
        } else {
            printInfoMessage(
                "§8[§c!§8] §rThe server §b{} §rwas not found. Press §bCTRL + C §rto §ccancel§r.",
                serverName
            );
        }
    }

    @Override
    public void tick(long currentTick) {
        checkLogStream();
        if (stream != null) {
            while (true) {
                String line = stream.readNewLine();
                if (line == null) break;
                line = processAnsi(line).trim();
                line = line.replaceFirst("^>+\\s*", "");
                if (line.isEmpty()) continue;
                printLog(line);
            }
        }
    }

    @Override
    public void onRemove(long currentTick) {
        if (stream != null) stream.stopStream();
        stream = null;

        clear();
        restoreAll();
        printLogCache();
    }

    @Override
    public InterruptionResult onCancel(long currentTick) {
        PocketCloud.instance().screens().reset();
        return InterruptionResult.CONTINUE;
    }

    private void printInfoMessage(String message, Object... params) {
        if (message.equals(lastInfoMessage)) return;
        logger.withoutFormat(message, params);
        lastInfoMessage = message;
    }

    private void printLog(String message) {
        logger.withoutFormat(message);
        lastInfoMessage = null;
    }
}