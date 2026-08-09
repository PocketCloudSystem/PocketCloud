package de.pocketcloud.bridge.platform.pnx.auth;

import org.powernukkitx.network.process.auth.ClientChainData;
import org.powernukkitx.network.process.auth.ProxyAuthProvider;

public class WaterdogProxyAuthProvider implements ProxyAuthProvider {

    @Override
    public boolean isUnsignedLoginAllowed() {
        return true;
    }

    @Override
    public String getXuid(ClientChainData chainData, String fallback) {
        Object xuid = chainData.getRawClaims().get("Waterdog_XUID");
        return xuid != null ? xuid.toString() : fallback;
    }
}