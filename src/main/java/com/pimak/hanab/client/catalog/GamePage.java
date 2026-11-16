package com.pimak.hanab.client.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

/** A paginated history response from the WebSocket protocol. */
public record GamePage(List<GameHistoryEntry> games, long offset, long total) {
    public GamePage {
        games = List.copyOf(Objects.requireNonNull(games, "games"));
        if (offset < 0 || total < 0 || offset > total || offset > 9_007_199_254_740_991L || total > 9_007_199_254_740_991L) throw new IllegalArgumentException("page counts are out of range");
        if (games.size() > Math.max(0, total - offset)) throw new IllegalArgumentException("page exceeds total game count");
    }
    public boolean hasNext() { return offset + games.size() < total; }

    public static GamePage from(JsonNode json) {
        if (json == null || !json.isObject() || !json.path("games").isArray()) throw new IllegalArgumentException("invalid game page");
        var entries = new java.util.ArrayList<GameHistoryEntry>();
        for (JsonNode game : json.get("games")) entries.add(entry(game));
        return new GamePage(entries, requiredLong(json, "offset"), requiredLong(json, "total"));
    }

    private static GameHistoryEntry entry(JsonNode json) {
        if (json == null || !json.isObject() || !json.path("playerNames").isArray() || !json.path("tags").isArray()) throw new IllegalArgumentException("invalid game history entry");
        JsonNode options=json.get("options"); if(options==null || !options.isObject()) throw new IllegalArgumentException("missing object field: options");
        return new GameHistoryEntry(requiredLong(json, "gameId"), options, requiredText(json, "seed"), requiredLong(json, "score"), requiredLong(json, "numTurns"), requiredText(json, "endCondition"), requiredText(json, "startedAt"), requiredText(json, "finishedAt"), requiredLong(json, "numGamesOnSeed"), strings(json.get("playerNames")), strings(json.get("tags")));
    }
    private static long requiredLong(JsonNode json, String name) { JsonNode value = json.get(name); if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) throw new IllegalArgumentException("invalid integer field: " + name); return value.longValue(); }
    private static String requiredText(JsonNode json, String name) { JsonNode value = json.get(name); if (value == null || !value.isTextual()) throw new IllegalArgumentException("missing text field: " + name); return value.textValue(); }
    private static List<String> strings(JsonNode values) { var result = new java.util.ArrayList<String>(); for (JsonNode value : values) { if (!value.isTextual()) throw new IllegalArgumentException("expected text list"); result.add(value.textValue()); } return result; }
}
