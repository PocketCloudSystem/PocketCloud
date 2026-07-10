package de.pocketcloud.network.packet.type;

import de.pocketcloud.common.serialization.Writable;

public enum TextType implements Writable<String> {

    MESSAGE,
    POPUP,
    TIP,
    TITLE,
    ACTION_BAR,
    TOAST;

    @Override
    public String write() {
        return name();
    }
}