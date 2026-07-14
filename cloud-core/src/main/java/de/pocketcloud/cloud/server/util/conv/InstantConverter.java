package de.pocketcloud.cloud.server.util.conv;

import de.pocketcloud.common.mapper.MapKeyConverter;

import java.time.Instant;

public class InstantConverter implements MapKeyConverter<Instant, Long> {

    @Override
    public Long toValue(Instant obj) {
        return obj.toEpochMilli();
    }

    @Override
    public Instant fromValue(Long value) {
        return Instant.ofEpochMilli(value);
    }
}