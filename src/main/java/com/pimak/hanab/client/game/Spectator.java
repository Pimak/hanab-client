package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;

/** A spectator entry published by the server, retaining future fields. */
public record Spectator(String username, java.util.Optional<Long> userId, JsonNode raw) {
    public Spectator {
        if (username == null || username.isBlank() || userId == null || raw == null) throw new NullPointerException();
        raw = raw.deepCopy();
    }
    public static Spectator from(JsonNode node) {
        if (node == null || node.isNull()) throw new IllegalArgumentException("spectator must not be null");
        if (!node.isObject()) throw new IllegalArgumentException("spectator must be an object or text");
        JsonNode name = node.has("username") ? node.get("username") : node.get("name");
        if (name == null || !name.isTextual() || name.asText().isBlank()) throw new IllegalArgumentException("spectator username is required");
        var id = node.get("userId");
        if (id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() < 1) throw new IllegalArgumentException("spectator userId is required");
        return new Spectator(name.asText(), java.util.Optional.of(id.longValue()), node);
    }
    private static java.util.Optional<Long> optionalId(JsonNode node) {
        if (node == null || node.isNull()) return java.util.Optional.empty();
        if (!node.isIntegralNumber() || !node.canConvertToLong() || node.longValue() < 0) throw new IllegalArgumentException("invalid spectator userId");
        return java.util.Optional.of(node.longValue());
    }
}
