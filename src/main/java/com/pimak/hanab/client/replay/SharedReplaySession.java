package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;
import java.time.Instant;

/** Immutable local projection of a shared replay's collaboration state. */
public record SharedReplaySession(String owner, int turn, boolean spoilerFree,
                                  boolean canPlayAgain, List<ReplayAnnotation> annotations,
                                  java.util.Optional<String> replayStartedAt, java.util.OptionalInt efficiencyModifier,
                                  List<String> restartDeclined) {
    public SharedReplaySession {
        if (owner == null || owner.isBlank() || turn < 0 || turn > 512) throw new IllegalArgumentException("invalid shared replay state");
        annotations = List.copyOf(Objects.requireNonNull(annotations, "annotations"));
        replayStartedAt = Objects.requireNonNull(replayStartedAt, "replayStartedAt");
        efficiencyModifier = Objects.requireNonNull(efficiencyModifier, "efficiencyModifier");
        restartDeclined = List.copyOf(Objects.requireNonNull(restartDeclined, "restartDeclined"));
        if (annotations.size() > 256 || restartDeclined.size() > 6) throw new IllegalArgumentException("shared replay collections are too large");
        if (efficiencyModifier.isPresent() && (efficiencyModifier.getAsInt() < -60 || efficiencyModifier.getAsInt() > 60)) throw new IllegalArgumentException("efficiency modifier is out of range");
        replayStartedAt.ifPresent(value -> { if (!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")) throw new IllegalArgumentException("invalid replayStartedAt"); try { Instant.parse(value); } catch (RuntimeException error) { throw new IllegalArgumentException("invalid replayStartedAt", error); } });
    }
    public SharedReplaySession(String owner, int turn, boolean spoilerFree, boolean canPlayAgain) {
        this(owner, turn, spoilerFree, canPlayAgain, List.of(), java.util.Optional.empty(), java.util.OptionalInt.empty(), List.of());
    }
    public SharedReplaySession atTurn(int nextTurn) { return new SharedReplaySession(owner, nextTurn, spoilerFree, canPlayAgain, annotations, replayStartedAt, efficiencyModifier, restartDeclined); }
    public SharedReplaySession withAnnotation(ReplayAnnotation annotation) {
        var next = new java.util.ArrayList<>(annotations); next.add(Objects.requireNonNull(annotation));
        return new SharedReplaySession(owner, turn, spoilerFree, canPlayAgain, next, replayStartedAt, efficiencyModifier, restartDeclined);
    }
    /** Decodes the shared-replay portion of a game mode snapshot. */
    public static SharedReplaySession fromMode(JsonNode mode) {
        if (mode == null || !mode.isObject() || !mode.path("type").isTextual() || !mode.path("type").textValue().equals("sharedReplay")) throw new IllegalArgumentException("mode is not a shared replay");
        rejectUnknown(mode, java.util.Set.of("type", "sharedReplay", "gameId", "ourPlayerIndex"));
        if (mode.has("gameId") && (!mode.path("gameId").canConvertToInt() || mode.path("gameId").intValue() < 1)) throw new IllegalArgumentException("invalid shared replay gameId");
        if (mode.has("ourPlayerIndex") && (!mode.path("ourPlayerIndex").canConvertToInt() || mode.path("ourPlayerIndex").intValue() < 0 || mode.path("ourPlayerIndex").intValue() > 5)) throw new IllegalArgumentException("invalid shared replay player index");
        var state = mode.path("sharedReplay"); if (!state.isObject()) throw new IllegalArgumentException("sharedReplay state is missing");
        rejectUnknown(state, java.util.Set.of("owner", "turn", "efficiencyModifier", "spoilerFree", "restartDeclined", "canPlayAgain", "replayStartedAt", "annotations"));
        var ownerNode = state.path("owner"); var ownerId = ownerNode.path("userId"); var owner = ownerNode.path("username"); if (!ownerNode.isObject() || !ownerId.isIntegralNumber() || ownerId.longValue() < 1 || ownerId.longValue() > Integer.MAX_VALUE || !owner.isTextual() || owner.textValue().isBlank()) throw new IllegalArgumentException("shared replay owner is invalid");
        var turn = state.path("turn"); var modifier = state.path("efficiencyModifier");
        var spoilerFree = state.path("spoilerFree"); var canPlayAgain = state.path("canPlayAgain");
        if (!turn.canConvertToInt() || turn.intValue() < 0 || turn.intValue() > 512 || !modifier.canConvertToInt() || modifier.intValue() < -60 || modifier.intValue() > 60 || !spoilerFree.isBoolean() || !canPlayAgain.isBoolean()) throw new IllegalArgumentException("shared replay state is invalid");
        var declined = new java.util.ArrayList<String>(); var values = state.path("restartDeclined"); if (!values.isArray() || values.size() > 6) throw new IllegalArgumentException("restartDeclined is invalid"); values.forEach(value -> { if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("invalid declined player"); declined.add(value.textValue()); });
        var started = state.path("replayStartedAt"); if (!started.isTextual() || !started.textValue().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}Z")) throw new IllegalArgumentException("invalid replayStartedAt"); try { Instant.parse(started.textValue()); } catch (RuntimeException error) { throw new IllegalArgumentException("invalid replayStartedAt", error); }
        var annotations = new java.util.ArrayList<ReplayAnnotation>();
        var annotationValues = state.path("annotations");
        if (!annotationValues.isMissingNode() && !annotationValues.isArray()) throw new IllegalArgumentException("annotations is invalid");
        if (annotationValues.isArray()) { if (annotationValues.size() > 256) throw new IllegalArgumentException("too many annotations"); annotationValues.forEach(value -> {
            if (!value.isObject() || !value.path("kind").isTextual() || value.path("kind").textValue().isBlank() || !value.has("value")) throw new IllegalArgumentException("invalid replay annotation");
            annotations.add(new ReplayAnnotation(value.path("kind").textValue(), value.get("value")));
        }); }
        return new SharedReplaySession(owner.textValue(), turn.intValue(), spoilerFree.booleanValue(), canPlayAgain.booleanValue(), annotations, java.util.Optional.of(started.textValue()), java.util.OptionalInt.of(modifier.intValue()), declined);
    }
    private static void rejectUnknown(JsonNode object, java.util.Set<String> known) { var fields = object.fieldNames(); while (fields.hasNext()) if (!known.contains(fields.next())) throw new IllegalArgumentException("unknown shared replay field"); }
    public record ReplayAnnotation(String kind, JsonNode value) {
        public ReplayAnnotation { if (kind == null || kind.isBlank() || value == null) throw new NullPointerException(); value = value.deepCopy(); }
        @Override public JsonNode value() { return value.deepCopy(); }
    }
}
