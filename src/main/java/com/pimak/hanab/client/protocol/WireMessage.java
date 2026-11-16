package com.pimak.hanab.client.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** A normalized protocol message exposed to the client event stream. */
public record WireMessage(String command, JsonNode payload, Kind kind, long requestId) {
    public enum Kind { EVENT, REPLY }
    public WireMessage(String command, JsonNode payload) { this(command, payload, Kind.EVENT, 0); }
    public WireMessage { if (command == null || !command.matches("[A-Za-z][A-Za-z0-9]*")) throw new IllegalArgumentException("invalid command"); if (payload == null || !payload.isObject()) throw new IllegalArgumentException("payload must be an object"); }
}
