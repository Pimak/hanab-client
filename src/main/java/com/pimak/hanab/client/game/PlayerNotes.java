package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Notes belonging to one player in the server game-state representation. */
public record PlayerNotes(String username, List<String> notes, boolean spectator, JsonNode raw) {
    public PlayerNotes {
        if (username == null || username.isBlank() || notes == null || raw == null) throw new IllegalArgumentException("invalid player notes");
        notes = List.copyOf(notes); raw = raw.deepCopy();
    }
    public static PlayerNotes from(JsonNode node) {
        if (node == null || !node.isObject() || !node.has("username") || !node.path("notes").isArray()) throw new IllegalArgumentException("invalid player notes");
        if (!node.get("username").isTextual() || node.get("username").asText().isBlank()) throw new IllegalArgumentException("invalid player notes username");
        var values = new java.util.ArrayList<String>();
        node.path("notes").forEach(value -> { if (!value.isTextual()) throw new IllegalArgumentException("notes must contain text"); values.add(value.asText()); });
        boolean spectator = !node.has("spectator") || node.get("spectator").isNull() ? false
                : node.get("spectator").isBoolean() ? node.get("spectator").booleanValue() : invalidSpectator();
        return new PlayerNotes(node.get("username").asText(), values, spectator, node);
    }
    private static boolean invalidSpectator() { throw new IllegalArgumentException("spectator must be boolean"); }
}
