package de.pocketcloud.cloud.cluster.bridge.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Any node → owning node: do something with one of your servers and answer with a
 * {@link NodeServerActionResultPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeServerActionPacket extends RequestPacket {

    public enum Action {
        /** Run a command on the server ({@code argument}: command line). */
        COMMAND,
        /** Read the log file ({@code argument}: first line, as number). */
        LOGS,
        /** Save the server (save command + savable files). */
        SAVE,
        /** Status change reported by a bridge of another node ({@code argument}: status name). */
        STATUS,
        /** Storage change reported by a bridge of another node ({@code data}: the storage). */
        STORAGE
    }

    private String serverName;
    private Action action;
    private String argument;
    private Map<String, Object> data;

    public NodeServerActionPacket(String serverName, Action action, String argument, Map<String, Object> data) {
        this.serverName = serverName;
        this.action = action;
        this.argument = argument;
        this.data = data;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName, action, argument, data);
    }

    @SuppressWarnings("SameReturnValue")
    static <T> T nullValue(IPacketData data) {
        data.read();
        return null;
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        serverName = packetData.readString();
        action = packetData.readEnum(Action.class);
        argument = packetData.readString();
        data = packetData.peek() == null ? nullValue(packetData) : packetData.readMap();
    }
}
