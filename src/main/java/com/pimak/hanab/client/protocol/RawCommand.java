package com.pimak.hanab.client.protocol;

import java.util.Map;
import java.util.Objects;
import java.util.Collections;
import java.util.LinkedHashMap;

/** Escape hatch for a command added by the server before a typed wrapper is released. */
public record RawCommand(String commandName, Map<String, Object> payload) implements OutboundCommand {
    public RawCommand {
        commandName = Objects.requireNonNull(commandName, "commandName");
        if (commandName.isBlank()) throw new IllegalArgumentException("commandName must not be blank");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(payload, "payload")));
    }
}
