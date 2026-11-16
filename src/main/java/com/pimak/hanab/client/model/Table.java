package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.pimak.hanab.client.game.Spectator;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.List;

/** A table snapshot. Unknown server fields remain available in {@code raw}. */
public record Table(Ids.TableId id, String phase, String owner, JsonNode raw) {
    public Table { if (id == null || phase == null || raw == null) throw new NullPointerException(); raw = raw.deepCopy(); }
    public Table(long tableId, String name, java.util.List<User> players, java.util.Map<String,Object> unknown) {
        this(new Ids.TableId(tableId), "unknown", players == null || players.isEmpty() ? null : players.get(0).username(),
                new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(java.util.Map.of("name", name, "players", players == null ? java.util.List.of() : players, "unknown", unknown == null ? java.util.Map.of() : unknown)));
    }
    @Override public JsonNode raw() { return raw.deepCopy(); }
    public java.util.List<String> playerNames() {
        var values = raw.path("playerNames");
        if (!values.isArray()) values = raw.path("players");
        if (!values.isArray()) throw new IllegalStateException("table players must be an array");
        var result = new java.util.ArrayList<String>();
        values.forEach(value -> {
            String name = value.isTextual() ? value.textValue() : value.isObject() && value.path("username").isTextual() ? value.path("username").textValue() : null;
            if (name == null || name.isBlank()) throw new IllegalStateException("table player must have a username");
            result.add(name);
        });
        return java.util.List.copyOf(result);
    }
    /** Structured owner when the server includes the full user object. */
    public Optional<User> ownerUser() {
        JsonNode value = raw.get("owner");
        if (value == null || value.isNull()) return Optional.empty();
        if (!value.isObject() || !value.has("userId") || !value.has("username") || !value.get("userId").canConvertToLong())
            throw new IllegalStateException("table owner is malformed");
        return Optional.of(new User(value.get("userId").longValue(), value.get("username").textValue()));
    }
    public Optional<TableSettings> settings() {
        JsonNode value = raw.get("settings");
        return value == null || value.isNull() ? Optional.empty() : Optional.of(TableSettings.fromJson(value));
    }
    public Optional<Boolean> passwordProtected() {
        JsonNode value = raw.get("passwordProtected");
        if (value == null || value.isNull()) return Optional.empty();
        if (!value.isBoolean()) throw new IllegalStateException("passwordProtected must be boolean");
        return Optional.of(value.booleanValue());
    }
    public List<Spectator> spectators() {
        JsonNode values = raw.get("spectators");
        if (values == null || values.isNull()) return List.of();
        if (!values.isArray()) throw new IllegalStateException("table spectators must be an array");
        var result = new java.util.ArrayList<Spectator>();
        values.forEach(value -> result.add(Spectator.from(value)));
        return List.copyOf(result);
    }
    public OptionalInt progress() {
        JsonNode value = raw.get("progress");
        if (value == null || value.isNull()) return OptionalInt.empty();
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0) throw new IllegalStateException("table progress is invalid");
        return OptionalInt.of(value.intValue());
    }
}
