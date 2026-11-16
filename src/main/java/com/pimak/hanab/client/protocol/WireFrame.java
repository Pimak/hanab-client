package com.pimak.hanab.client.protocol;

import com.fasterxml.jackson.databind.JsonNode;

/** Structured WebSocket frame used by the current Hanab Live protocol. */
public record WireFrame(Kind kind, long id, String name, JsonNode data, JsonNode error) {
    public enum Kind { EVENT, REPLY }

    public WireFrame {
        if (kind == null) throw new IllegalArgumentException("frame kind must not be null");
        if (kind == Kind.REPLY && (id < 1 || id > 9_007_199_254_740_991L))
            throw new IllegalArgumentException("reply id must be a safe positive integer");
        if (name == null || name.isBlank() || !name.matches("[A-Za-z][A-Za-z0-9]*"))
            throw new IllegalArgumentException("frame name must be a valid protocol name");
        if (data != null && !data.isObject()) throw new IllegalArgumentException("frame data must be an object");
        if (error != null && !error.isObject() && !error.isTextual())
            throw new IllegalArgumentException("frame error must be an object or string");
    }

    public static WireFrame event(String name, JsonNode data) { return new WireFrame(Kind.EVENT, 0, name, data, null); }
    public static WireFrame reply(long id, String request, JsonNode data, JsonNode error) {
        return new WireFrame(Kind.REPLY, id, request, data, error);
    }
}
