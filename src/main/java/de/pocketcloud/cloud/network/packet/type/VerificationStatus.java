package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.util.Writable;

public enum VerificationStatus implements Writable<String> {

    DENIED,
    VERIFIED,
    PENDING;

    @Override
    public String write() {
        return name();
    }
}