package com.pimak.hanab.client.protocol;

import java.util.Map;
import java.util.Collections;
import java.util.LinkedHashMap;

/** The exact WebSocket envelope used by outbound commands. */
public record CommandEnvelope(String command, Map<String, Object> payload) {
    public CommandEnvelope {
        if (command == null || command.isBlank()) throw new IllegalArgumentException("command must not be blank");
        payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
    public static CommandEnvelope of(OutboundCommand command) {
        return new CommandEnvelope(command.commandName(), command.payload());
    }
}
