package de.pocketcloud.api.server;

import de.pocketcloud.common.serialization.Writable;

public enum VerificationStatus implements Writable<String> {

    DENIED,
    VERIFIED,
    PENDING;

    public boolean isVerified() {
        return this == VERIFIED;
    }

    public boolean isDenied() {
        return this == DENIED;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    @Override
    public String write() {
        return name();
    }
}