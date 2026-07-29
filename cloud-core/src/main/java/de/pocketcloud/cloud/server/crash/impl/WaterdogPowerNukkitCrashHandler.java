package de.pocketcloud.cloud.server.crash.impl;

import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.crash.CrashData;
import de.pocketcloud.cloud.server.crash.CrashHandler;
import de.pocketcloud.cloud.server.util.LatestPacketInfo;
import de.pocketcloud.network.packet.impl.DisconnectPacket;

import java.util.List;

/**
 * This crash handler doesn't really check if the server ACTUALLY crashed.
 * Unlike PocketMine, WaterdogPE and PowerNukkitX both do not provide crashdumps.
 */
public final class WaterdogPowerNukkitCrashHandler implements CrashHandler {

    @Override
    public CrashData retrieveCrashData(CloudServer server) {
        LatestPacketInfo latestPacketInfo = server.latestPacketInfo();
        boolean alive = server.isAlive();
        if (!alive) {
            if (latestPacketInfo.getReceivedPacketClass() != DisconnectPacket.class) {
                return CrashData.noInfo(server, true);
            }
        }

        return CrashData.noCrash(server);
    }

    @Override
    public List<IServerSoftware> applicableSoftware() {
        return List.of(
                PocketCloud.instance().softwares().get("waterdogpe-latest").orElseThrow(() -> new RuntimeException("Required software not found")),
                PocketCloud.instance().softwares().get("powernukkitx-latest").orElseThrow(() -> new RuntimeException("Required software not found"))
        );
    }
}