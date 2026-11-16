package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/** Server-owned state surrounding the ordered game events, retained without schema loss. */
public record GameStateMetadata(List<Boolean> connected, JsonNode notes, JsonNode spectators,
                                Optional<JsonNode> votes, Optional<JsonNode> clocks,
                                Optional<JsonNode> pause, Optional<JsonNode> hypothetical,
                                JsonNode unknownFields) {
    public GameStateMetadata {
        connected = List.copyOf(Objects.requireNonNull(connected, "connected"));
        notes = copy(Objects.requireNonNull(notes, "notes"));
        spectators = copy(Objects.requireNonNull(spectators, "spectators"));
        votes = copyOptional(votes);
        clocks = copyOptional(clocks);
        pause = copyOptional(pause);
        hypothetical = copyOptional(hypothetical);
        unknownFields = copy(Objects.requireNonNull(unknownFields, "unknownFields"));
    }
    @Override public JsonNode notes() { return notes.deepCopy(); }
    @Override public JsonNode spectators() { return spectators.deepCopy(); }
    @Override public Optional<JsonNode> votes() { return votes.map(JsonNode::deepCopy); }
    @Override public Optional<JsonNode> clocks() { return clocks.map(JsonNode::deepCopy); }
    @Override public Optional<JsonNode> pause() { return pause.map(JsonNode::deepCopy); }
    @Override public Optional<JsonNode> hypothetical() { return hypothetical.map(JsonNode::deepCopy); }
    @Override public JsonNode unknownFields() { return unknownFields.deepCopy(); }

    /** Typed notes, when the server publishes the canonical note list shape. */
    public List<CardNote> cardNotes() {
        var result = new java.util.ArrayList<CardNote>();
        if (!notes.isArray()) throw new IllegalStateException("notes must be an array");
        notes.forEach(node -> {
            if (node.isObject() && node.path("order").isIntegralNumber() && node.path("text").isTextual()) { result.add(CardNote.from(node)); return; }
            var owner = PlayerNotes.from(node);
            for (int order = 0; order < owner.notes().size(); order++) {
                if (owner.notes().get(order).isEmpty()) continue;
                var raw = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()
                        .put("order", order).put("text", owner.notes().get(order))
                        .put("username", owner.username()).put("spectator", owner.spectator());
                result.add(CardNote.from(raw));
            }
        });
        return List.copyOf(result);
    }
    public List<Spectator> spectatorList() {
        var result = new java.util.ArrayList<Spectator>();
        if (!spectators.isArray()) throw new IllegalStateException("spectators must be an array");
        spectators.forEach(node -> result.add(Spectator.from(node)));
        return List.copyOf(result);
    }
    public List<PlayerNotes> playerNotes() {
        var result = new java.util.ArrayList<PlayerNotes>();
        if (!notes.isArray()) throw new IllegalStateException("notes must be an array");
        notes.forEach(node -> result.add(PlayerNotes.from(node)));
        return List.copyOf(result);
    }
    public Optional<GameClock> clock() { return clocks.map(node -> GameClock.from(node)); }
    public Optional<GameStats> stats() {
        for (String key : List.of("stats", "statistics", "gameStats")) if (unknownFields.has(key) && !unknownFields.get(key).isNull()) return Optional.of(GameStats.from(unknownFields.get(key)));
        return Optional.empty();
    }
    public Perspective perspective() { return perspective(JsonNodeFactoryHolder.EMPTY); }
    public Perspective perspective(JsonNode init) {
        JsonNode mode = init.path("mode");
        String fallback = unknownFields.path("perspective").asText(unknownFields.path("mode").asText("unknown"));
        String value = mode.isObject() ? mode.path("type").asText("unknown") : mode.asText(fallback);
        return Perspective.from(value);
    }
    public OptionalInt pausingPlayerIndex() { return pause.isPresent() ? optionalInt(pause.get(), "pausingPlayerIndex") : OptionalInt.empty(); }
    public boolean pauseQueued() { if (pause.isEmpty() || !pause.get().has("queued")) return false; var value = pause.get().get("queued"); if (!value.isBoolean()) throw new IllegalArgumentException("pause.queued must be boolean"); return value.booleanValue(); }
    private static OptionalInt optionalInt(JsonNode node, String field) { if (!node.has(field) || node.get(field).isNull()) return OptionalInt.empty(); var value=node.get(field); if (!value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0 || value.intValue() > 5) throw new IllegalArgumentException("invalid pause player index"); return OptionalInt.of(value.intValue()); }
    private static final class JsonNodeFactoryHolder {
        static final JsonNode EMPTY = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        static final JsonNode EMPTY_ARRAY = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.arrayNode();
    }

    static GameStateMetadata from(JsonNode state) { return from(state, JsonNodeFactoryHolder.EMPTY); }
    static GameStateMetadata from(JsonNode state, JsonNode init) {
        var connected = new java.util.ArrayList<Boolean>();
        JsonNode connectedNode = state.has("connected") && !state.get("connected").isNull() ? state.get("connected") : JsonNodeFactoryHolder.EMPTY_ARRAY;
        if (!connectedNode.isArray() || connectedNode.size() < 2 || connectedNode.size() > 6) throw new IllegalArgumentException("connected must contain two to six players");
        connectedNode.forEach(value -> { if (!value.isBoolean()) throw new IllegalArgumentException("connected must contain booleans"); connected.add(value.booleanValue()); });
        var known = java.util.Set.of("tableId", "events", "deck", "notes", "votes", "connected", "clocks", "pause", "spectators", "hypothetical");
        var unknown = com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode();
        state.fields().forEachRemaining(entry -> { if (!known.contains(entry.getKey())) unknown.set(entry.getKey(), entry.getValue().deepCopy()); });
        if (init != null && init.has("mode")) unknown.set("mode", init.get("mode").deepCopy());
        JsonNode notes = arrayOrEmpty(state, "notes");
        JsonNode spectators = arrayOrEmpty(state, "spectators");
        return new GameStateMetadata(connected, notes, spectators, optional(state, "votes"), optional(state, "clocks"), optional(state, "pause"), optional(state, "hypothetical"), unknown);
    }

    private static JsonNode arrayOrEmpty(JsonNode state, String field) {
        if (!state.has(field) || state.get(field).isNull()) return JsonNodeFactoryHolder.EMPTY_ARRAY;
        if (!state.get(field).isArray()) throw new IllegalArgumentException(field + " must be an array");
        return state.get(field);
    }
    private static Optional<JsonNode> optional(JsonNode state, String field) { return state.has(field) && !state.get(field).isNull() ? Optional.of(state.get(field)) : Optional.empty(); }
    private static Optional<JsonNode> copyOptional(Optional<JsonNode> value) { return Objects.requireNonNull(value, "optional").map(GameStateMetadata::copy); }
    private static JsonNode copy(JsonNode value) { return value.deepCopy(); }
}
