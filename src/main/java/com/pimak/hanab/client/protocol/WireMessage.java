package com.pimak.hanab.client.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** The Hanab text frame: command, one space, then a JSON object. */
public record WireMessage(String command, JsonNode payload) {
    public WireMessage { if (command == null || !command.matches("[A-Za-z][A-Za-z0-9]*")) throw new IllegalArgumentException("invalid command"); if (payload == null || !payload.isObject()) throw new IllegalArgumentException("payload must be an object"); }
}
