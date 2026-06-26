package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.util.Writable;

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