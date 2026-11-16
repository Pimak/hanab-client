package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable settings for a pregame table. Game options remain extensible protocol data. */
public record TableSettings(String name, int maxPlayers, JsonNode options, Optional<JsonNode> deal) {
    private static final ObjectMapper JSON = new ObjectMapper();

    public TableSettings {
        if (name == null || name.isBlank() || name.length() > 45 || !name.equals(name.trim()) || name.chars().anyMatch(c -> c < 0x20 || "<>/\\\"".indexOf(c) >= 0)) throw new IllegalArgumentException("table name contains invalid characters");
        if (maxPlayers < 2 || maxPlayers > 6) throw new IllegalArgumentException("maxPlayers must be between 2 and 6");
        options = Objects.requireNonNull(options, "options").deepCopy();
        if (!options.isObject()) throw new IllegalArgumentException("table options must be an object");
        deal = Objects.requireNonNull(deal, "deal").map(value -> { validateDeal(value); return value.deepCopy(); });
    }

    @Override public JsonNode options() { return options.deepCopy(); }
    @Override public Optional<JsonNode> deal() { return deal.map(JsonNode::deepCopy); }

    public TableSettings(String name, int maxPlayers, JsonNode options) {
        this(name, maxPlayers, options, Optional.empty());
    }
    public TableSettings(String name, int maxPlayers, GameOptions options) {
        this(name, maxPlayers, Objects.requireNonNull(options, "options").toJson(), Optional.empty());
    }

    public static TableSettings named(String name) {
        return new TableSettings(name, 2, JSON.createObjectNode());
    }

    public JsonNode toJson() {
        ObjectNode result = JSON.createObjectNode().put("name", name).put("maxPlayers", maxPlayers);
        result.set("options", options.deepCopy());
        deal.ifPresent(value -> result.set("deal", value.deepCopy()));
        return result;
    }

    public static TableSettings fromJson(JsonNode value) {
        Objects.requireNonNull(value, "value");
        if (!value.isObject() || !value.has("name") || !value.get("name").isTextual()
                || !value.has("maxPlayers") || !value.get("maxPlayers").isIntegralNumber()
                || !value.get("maxPlayers").canConvertToInt() || !value.has("options") || !value.get("options").isObject())
            throw new IllegalArgumentException("invalid table settings");
        rejectUnknown(value, java.util.Set.of("name", "maxPlayers", "options", "deal"));
        if (value.has("deal") && value.get("deal").isNull()) throw new IllegalArgumentException("deal must be absent or an object");
        Optional<JsonNode> deal = value.has("deal") ? Optional.of(value.get("deal")) : Optional.empty();
        return new TableSettings(value.get("name").textValue(), value.get("maxPlayers").intValue(), value.get("options"), deal);
    }
    public GameOptions gameOptions() { return GameOptions.fromJson(options); }

    private static void validateDeal(JsonNode value) {
        if (value == null || !value.isObject() || !value.path("type").isTextual()) throw new IllegalArgumentException("invalid table deal");
        switch (value.path("type").textValue()) {
            case "seed" -> {
                rejectUnknown(value, java.util.Set.of("type", "suffix"));
                JsonNode seed = value.get("suffix");
                if (seed == null || !seed.isTextual() || !seed.textValue().matches("[A-Za-z0-9-]{1,32}")) throw new IllegalArgumentException("seed deal suffix must match [A-Za-z0-9-]{1,32}");
            }
            case "replay" -> {
                rejectUnknown(value, java.util.Set.of("type", "gameId", "turn"));
                JsonNode gameId = value.get("gameId");
                if (gameId == null || !gameId.isIntegralNumber() || !gameId.canConvertToInt() || gameId.intValue() < 1) throw new IllegalArgumentException("replay deal requires a positive gameId");
                JsonNode turn = value.get("turn");
                if (turn != null && (turn.isNull() || !turn.isIntegralNumber() || !turn.canConvertToInt() || turn.intValue() < 0 || turn.intValue() > 512)) throw new IllegalArgumentException("replay deal turn must be 0..512");
            }
            default -> throw new IllegalArgumentException("unknown table deal type");
        }
    }
    private static void rejectUnknown(JsonNode node, java.util.Set<String> known) { var fields = node.fieldNames(); while (fields.hasNext()) if (!known.contains(fields.next())) throw new IllegalArgumentException("unknown table settings field"); }
}
