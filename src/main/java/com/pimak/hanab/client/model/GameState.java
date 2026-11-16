package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/** Perspective-safe game state; card identities are present only when supplied by the server. */
public record GameState(Ids.TableId tableId, List<JsonNode> events, JsonNode raw) {
    public GameState { if (tableId == null || events == null || raw == null) throw new NullPointerException(); events = List.copyOf(events); raw = raw.deepCopy(); }
    public GameState(long tableId, List<String> eventNames, List<Integer> deck, int score, java.util.Map<String,Object> unknown) {
        this(new Ids.TableId(tableId), eventNames == null ? List.<JsonNode>of() : eventNames.stream().map(e -> (JsonNode) new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(e)).toList(),
                new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(java.util.Map.of("deck", deck == null ? List.of() : deck, "score", score, "unknown", unknown == null ? java.util.Map.of() : unknown)));
    }
}
