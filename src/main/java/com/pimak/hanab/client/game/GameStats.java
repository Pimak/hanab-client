package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.OptionalInt;

/** Lossless, tolerant projection of final/current game statistics. */
public record GameStats(OptionalInt score, OptionalInt maxScore, OptionalInt strikes, OptionalInt clueTokens,
                        OptionalInt deckSize, JsonNode raw) {
    public GameStats {
        if (score == null || maxScore == null || strikes == null || clueTokens == null || deckSize == null || raw == null) throw new NullPointerException();
        raw = raw.deepCopy();
    }
    public static GameStats from(JsonNode node) {
        return new GameStats(integer(node, "score"), integer(node, "maxScore"), integer(node, "strikes"), integer(node, "clueTokens"), integer(node, "deckSize"), node);
    }
    private static OptionalInt integer(JsonNode node, String name) { return node.has(name) && node.get(name).canConvertToInt() ? OptionalInt.of(node.get(name).asInt()) : OptionalInt.empty(); }
}
