package de.pocketcloud.shared.converter;

import de.pocketcloud.common.serialization.annotation.MapKeyConverter;

import java.time.Instant;

public final class InstantConverter implements MapKeyConverter<Instant, Long> {

    @Override
    public Long toValue(Instant obj) {
        return obj.toEpochMilli();
    }

    @Override
    public Instant fromValue(Long value) {
        return Instant.ofEpochMilli(value);
    }
}