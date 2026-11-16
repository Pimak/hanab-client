package com.pimak.hanab.client.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/** A finished game as returned by the history and seed pages. */
public record GameHistoryEntry(long gameId, JsonNode options, String seed, long score, long numTurns,
                               String endCondition, String startedAt, String finishedAt,
                               long numGamesOnSeed, List<String> playerNames, List<String> tags) {
    public GameHistoryEntry {
        if (gameId < 1 || score < 0 || numTurns < 0 || numGamesOnSeed < 0) throw new IllegalArgumentException("history values are out of range");
        options = Objects.requireNonNull(options, "options").deepCopy();
        seed = Objects.requireNonNull(seed, "seed");
        endCondition = Objects.requireNonNull(endCondition, "endCondition");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        finishedAt = Objects.requireNonNull(finishedAt, "finishedAt");
        try { if (!OffsetDateTime.parse(startedAt).getOffset().equals(ZoneOffset.UTC) || !OffsetDateTime.parse(finishedAt).getOffset().equals(ZoneOffset.UTC)) throw new IllegalArgumentException(); Instant.parse(startedAt); Instant.parse(finishedAt); } catch (RuntimeException e) { throw new IllegalArgumentException("history timestamps must be RFC 3339 UTC", e); }
        playerNames = List.copyOf(Objects.requireNonNull(playerNames, "playerNames"));
        if (playerNames.isEmpty() || playerNames.size() > 6 || playerNames.stream().anyMatch(name -> name == null || name.isBlank())) throw new IllegalArgumentException("playerNames must contain 1..6 non-blank names");
        tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
    }
    public static GameHistoryEntry from(JsonNode json) {
        if (json == null || !json.isObject() || !json.path("playerNames").isArray() || !json.path("tags").isArray()) throw new IllegalArgumentException("invalid game history entry");
        JsonNode options = json.get("options");
        if (options == null || !options.isObject()) throw new IllegalArgumentException("missing object field: options");
        return new GameHistoryEntry(requiredLong(json, "gameId"), options, requiredText(json, "seed"), requiredLong(json, "score"),
                requiredLong(json, "numTurns"), requiredText(json, "endCondition"), requiredText(json, "startedAt"),
                requiredText(json, "finishedAt"), requiredLong(json, "numGamesOnSeed"), strings(json.get("playerNames")), strings(json.get("tags")));
    }
    private static long requiredLong(JsonNode json, String name) { JsonNode value = json.get(name); if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()) throw new IllegalArgumentException("invalid integer field: " + name); return value.longValue(); }
    private static String requiredText(JsonNode json, String name) { JsonNode value = json.get(name); if (value == null || !value.isTextual()) throw new IllegalArgumentException("missing text field: " + name); return value.textValue(); }
    private static List<String> strings(JsonNode values) { var result = new java.util.ArrayList<String>(); for (JsonNode value : values) { if (!value.isTextual()) throw new IllegalArgumentException("expected text list"); result.add(value.textValue()); } return result; }
}
