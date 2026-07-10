package de.pocketcloud.network.packet.type;

import de.pocketcloud.common.serialization.Writable;

public enum LogType implements Writable<String> {

    INFO,
    WARN,
    ERROR,
    SUCCESS,
    DEBUG;

    @Override
    public String write() {
        return name();
    }
}