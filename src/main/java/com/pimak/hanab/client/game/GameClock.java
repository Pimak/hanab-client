package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.OptionalInt;

/** Server clock projection. Durations are milliseconds. */
public record GameClock(List<Long> playerTimesMs, OptionalInt currentPlayerIndex, long turnElapsedMs, JsonNode raw) {
    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    public GameClock {
        if (playerTimesMs == null || playerTimesMs.size() < 2 || playerTimesMs.size() > 6 || playerTimesMs.stream().anyMatch(value -> value == null || value < -MAX_SAFE_INTEGER || value > MAX_SAFE_INTEGER) || currentPlayerIndex == null || turnElapsedMs < 0 || turnElapsedMs > MAX_SAFE_INTEGER || raw == null) throw new IllegalArgumentException("invalid game clock");
        playerTimesMs = List.copyOf(playerTimesMs); raw = raw.deepCopy();
    }
    public static GameClock from(JsonNode node) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("clock must be an object");
        var times = new java.util.ArrayList<Long>();
        JsonNode values = node.path("playerTimesMs");
        if (!values.isArray()) throw new IllegalArgumentException("playerTimesMs must be an array");
        values.forEach(v -> { if (!v.isIntegralNumber() || !v.canConvertToLong() || v.longValue() < -MAX_SAFE_INTEGER || v.longValue() > MAX_SAFE_INTEGER) throw new IllegalArgumentException("invalid player time"); times.add(v.longValue()); });
        var current = node.has("currentPlayerIndex") && !node.get("currentPlayerIndex").isNull()
                ? OptionalInt.of(requiredNonNegativeInt(node.get("currentPlayerIndex"))) : OptionalInt.empty();
        long elapsed = node.has("turnElapsedMs") && !node.get("turnElapsedMs").isNull()
                ? requiredNonNegativeLong(node.get("turnElapsedMs")) : 0;
        return new GameClock(times, current, elapsed, node);
    }
    private static int requiredNonNegativeInt(JsonNode n) { if (!n.isIntegralNumber() || !n.canConvertToInt() || n.intValue() < 0 || n.intValue() > 5) throw new IllegalArgumentException("invalid player index"); return n.intValue(); }
    private static long requiredNonNegativeLong(JsonNode n) { if (!n.isIntegralNumber() || !n.canConvertToLong() || n.longValue() < 0 || n.longValue() > MAX_SAFE_INTEGER) throw new IllegalArgumentException("invalid elapsed time"); return n.longValue(); }
}
