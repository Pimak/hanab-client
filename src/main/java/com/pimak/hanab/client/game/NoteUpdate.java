package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Server note event: one card order and the per-player note values for it. */
public record NoteUpdate(int order, List<PlayerNote> notes, JsonNode raw) {
    public NoteUpdate {
        if (order < 0 || order > 65 || notes == null || raw == null) throw new IllegalArgumentException("invalid note update");
        notes = List.copyOf(notes); raw = raw.deepCopy();
    }
    public static NoteUpdate from(JsonNode node) {
        if (node == null || !node.isObject() || !node.path("notes").isArray()) throw new IllegalArgumentException("invalid note update");
        var values = new java.util.ArrayList<PlayerNote>(); node.path("notes").forEach(value -> values.add(PlayerNote.from(value)));
        var order = node.get("order"); if (order == null || !order.isIntegralNumber() || !order.canConvertToInt()) throw new IllegalArgumentException("invalid note order");
        return new NoteUpdate(order.intValue(), values, node);
    }
    public record PlayerNote(String username, String text, boolean spectator) {
        public PlayerNote { if (username == null || username.isBlank() || text == null) throw new IllegalArgumentException("invalid player note"); }
        static PlayerNote from(JsonNode node) { if (node == null || !node.isObject() || !node.path("username").isTextual() || node.path("username").textValue().isBlank() || !node.path("text").isTextual() || !node.path("spectator").isBoolean()) throw new IllegalArgumentException("invalid player note"); return new PlayerNote(node.path("username").textValue(), node.path("text").textValue(), node.path("spectator").booleanValue()); }
    }
}
