package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;

/** A note associated with a card order. */
public record CardNote(int order, String text, JsonNode raw) {
    public CardNote {
        if (order < 0 || text == null || raw == null) throw new IllegalArgumentException("invalid card note");
        raw = raw.deepCopy();
    }
    public static CardNote from(JsonNode node) {
        if (node == null || !node.isObject() || !node.has("order") || !node.get("order").isIntegralNumber()
                || !node.get("order").canConvertToInt() || !node.has("text") || !node.get("text").isTextual())
            throw new IllegalArgumentException("note must contain an integer order and text");
        return new CardNote(node.get("order").intValue(), node.get("text").asText(), node);
    }
}
