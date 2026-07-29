package de.pocketcloud.shared.network.packet.type;

import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.common.serialization.Writable;

import java.util.function.Supplier;

public enum ActionFailureReason implements Writable<String> {

    NONE,
    TEMPLATE_NOT_FOUND,
    MAX_SERVERS_REACHED,
    SERVER_NOT_FOUND,
    REQUEST_TIMEOUT();

    public String toMessage(Supplier<Object[]> argsSupplier) {
        LanguageKey key = toLangKey();
        if (key == null) return "";
        return key.translate(argsSupplier.get());
    }

    public String toMessage(Object... args) {
        return toMessage(() -> args);
    }

    public LanguageKey toLangKey() {
        return switch (this) {
            case TEMPLATE_NOT_FOUND -> LanguageKey.INGAME_TEMPLATE_NOT_FOUND;
            case MAX_SERVERS_REACHED -> LanguageKey.INGAME_MAX_SERVERS_REACHED;
            case SERVER_NOT_FOUND -> LanguageKey.INGAME_SERVER_NOT_FOUND;
            case REQUEST_TIMEOUT -> LanguageKey.INGAME_REQUEST_TIMED_OUT;
            case NONE -> null;
        };
    }

    @Override
    public String write() {
        return name();
    }
}