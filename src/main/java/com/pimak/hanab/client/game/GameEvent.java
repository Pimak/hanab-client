package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Optional;

/** Immutable, typed view of one server game event. {@link #raw()} is lossless for extensions. */
public sealed interface GameEvent permits GameEvent.CardEvent, GameEvent.ClueEvent,
        GameEvent.TurnEvent, GameEvent.GameOverEvent, GameEvent.PlayerTimesEvent, GameEvent.UnknownEvent {
    String type();
    JsonNode raw();

    record CardEvent(String type, int playerIndex, int order, Optional<CardIdentity> identity, JsonNode raw)
            implements GameEvent {
        public CardEvent {
            if (type == null || raw == null || identity == null) throw new NullPointerException();
            identity = identity.isPresent() ? Optional.of(identity.get()) : Optional.empty();
            raw = raw.deepCopy();
        }
        public boolean isDraw() { return type.equals("draw"); }
        public boolean isPlay() { return type.equals("play"); }
        public boolean isDiscard() { return type.equals("discard"); }
        public boolean isMisplay() { return type.equals("misplay"); }
    }

    record ClueEvent(int playerIndex, int receiverIndex, List<Integer> orders,
                     Optional<JsonNode> clue, Optional<Boolean> ignoreNegative, JsonNode raw)
            implements GameEvent {
        public ClueEvent {
            if (orders == null || clue == null || ignoreNegative == null || raw == null) throw new NullPointerException();
            orders = List.copyOf(orders);
            clue = clue.map(JsonNode::deepCopy);
            raw = raw.deepCopy();
        }
        @Override public String type() { return "clue"; }
    }

    record TurnEvent(int turn, Optional<Integer> currentPlayerIndex, JsonNode raw) implements GameEvent {
        public TurnEvent {
            if (currentPlayerIndex == null || raw == null) throw new NullPointerException();
            raw = raw.deepCopy();
        }
        @Override public String type() { return "turn"; }
    }

    record GameOverEvent(String endCondition, Optional<Integer> playerIndex, List<Integer> votes, JsonNode raw)
            implements GameEvent {
        public GameOverEvent {
            if (endCondition == null || playerIndex == null || votes == null || raw == null) throw new NullPointerException();
            votes = List.copyOf(votes);
            raw = raw.deepCopy();
        }
        @Override public String type() { return "gameOver"; }
    }

    record PlayerTimesEvent(long durationMs, List<Long> playerTimesMs, JsonNode raw) implements GameEvent {
        public PlayerTimesEvent {
            if (playerTimesMs == null || raw == null) throw new NullPointerException();
            playerTimesMs = List.copyOf(playerTimesMs);
            raw = raw.deepCopy();
        }
        @Override public String type() { return "playerTimes"; }
    }

    record UnknownEvent(String type, JsonNode raw) implements GameEvent {
        public UnknownEvent { if (type == null || raw == null) throw new NullPointerException(); raw = raw.deepCopy(); }
    }

    static GameEvent parse(JsonNode node) {
        if (node == null || !node.isObject()) throw new IllegalArgumentException("game event must be an object");
        String type = node.has("type")
                ? requiredText(node, "type")
                : "unknown";
        return switch (type) {
            case "draw", "play", "discard", "misplay", "cardIdentity" ->
                    new CardEvent(type, rangedInteger(node, "playerIndex", 0, 5), rangedInteger(node, "order", 0, 65),
                            card(node.get("identity")), node);
            case "clue" -> new ClueEvent(rangedInteger(node, "playerIndex", 0, 5), rangedInteger(node, "receiverIndex", 0, 5),
                    cardOrders(node, "orders"), optionalNode(node.get("clue")),
                    optionalBoolean(node, "ignoreNegative"), node);
            case "turn" -> new TurnEvent(rangedInteger(node, "turn", 0, 512), optionalSeat(node.get("currentPlayerIndex")), node);
            case "gameOver" -> new GameOverEvent(requiredEndCondition(node),
                    optionalSeat(node.get("playerIndex")), seatList(node, "votes"), node);
            case "playerTimes" -> new PlayerTimesEvent(requiredNonNegativeLong(node, "durationMs"),
                    requiredLongs(node, "playerTimesMs", false), node);
            default -> new UnknownEvent(type, node);
        };
    }

    private static Optional<String> text(JsonNode n, String name) { return n.hasNonNull(name) ? Optional.of(n.get(name).asText()) : Optional.empty(); }
    private static String requiredText(JsonNode n, String name) {
        JsonNode value = n.get(name);
        if (value == null || !value.isTextual() || value.asText().isBlank())
            throw new IllegalArgumentException("game event field must be non-empty text: " + name);
        return value.asText();
    }
    private static int requiredInteger(JsonNode n, String name) {
        JsonNode value = n.get(name);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) throw new IllegalArgumentException("game event field is out of range: " + name);
        if (value.intValue() < 0) throw new IllegalArgumentException("game event field must be non-negative: " + name);
        return value.intValue();
    }
    private static int rangedInteger(JsonNode n, String name, int min, int max) {
        int value = requiredInteger(n, name);
        if (value < min || value > max) throw new IllegalArgumentException("game event field is out of range: " + name);
        return value;
    }
    private static Optional<Integer> optionalSeat(JsonNode n) {
        if (n == null || n.isNull()) return Optional.empty();
        if (!n.isIntegralNumber() || !n.canConvertToInt() || n.intValue() < 0 || n.intValue() > 5) throw new IllegalArgumentException("seat is out of range");
        return Optional.of(n.intValue());
    }
    private static String requiredEndCondition(JsonNode n) {
        String value = requiredText(n, "endCondition");
        if (!java.util.Set.of("normal", "strikeout", "timeout", "terminatedByPlayer", "speedrunFail", "idleTimeout", "characterSoftlock", "allOrNothingFail", "allOrNothingSoftlock", "terminatedByVote").contains(value)) throw new IllegalArgumentException("unknown game end condition");
        return value;
    }
    private static Optional<Integer> optionalInteger(JsonNode n) {
        if (n == null || n.isNull()) return Optional.empty();
        if (!n.isIntegralNumber() || !n.canConvertToInt() || n.intValue() < 0) return throwInvalidInteger();
        return Optional.of(n.intValue());
    }
    private static Optional<Integer> throwInvalidInteger() { throw new IllegalArgumentException("optional integer is out of range"); }
    private static List<Integer> requiredIntegers(JsonNode parent, String name, boolean nonNegative) {
        JsonNode n = parent.get(name);
        if (n == null || !n.isArray()) throw new IllegalArgumentException("game event field must be an array: " + name);
        var out = new java.util.ArrayList<Integer>();
        n.forEach(v -> {
            if (!v.isIntegralNumber() || !v.canConvertToInt() || (nonNegative && v.intValue() < 0))
                throw new IllegalArgumentException("integer list value is out of range: " + name);
            out.add(v.intValue());
        });
        return out;
    }
    private static List<Integer> seatList(JsonNode parent, String name) {
        var values = requiredIntegers(parent, name, true);
        if (values.stream().anyMatch(value -> value > 5)) throw new IllegalArgumentException("seat list value is out of range: " + name);
        return values;
    }
    private static List<Integer> cardOrders(JsonNode parent, String name) {
        var values = requiredIntegers(parent, name, true);
        if (values.stream().anyMatch(value -> value > 65)) throw new IllegalArgumentException("card order is out of range: " + name);
        return values;
    }
    private static List<Long> requiredLongs(JsonNode parent, String name, boolean nonNegative) {
        JsonNode n = parent.get(name);
        if (n == null || !n.isArray()) throw new IllegalArgumentException("game event field must be an array: " + name);
        var out = new java.util.ArrayList<Long>();
        n.forEach(v -> {
            if (!v.isIntegralNumber() || !v.canConvertToLong() || (nonNegative && v.longValue() < 0))
                throw new IllegalArgumentException("long list value is out of range: " + name);
            out.add(v.longValue());
        });
        return out;
    }
    private static long requiredNonNegativeLong(JsonNode parent, String name) {
        JsonNode n = parent.get(name);
        if (n == null || !n.isIntegralNumber() || !n.canConvertToLong() || n.longValue() < 0)
            throw new IllegalArgumentException("game event field must be a non-negative long: " + name);
        return n.longValue();
    }
    private static Optional<Boolean> optionalBoolean(JsonNode parent, String name) {
        JsonNode n = parent.get(name);
        if (n == null || n.isNull()) return Optional.empty();
        if (!n.isBoolean()) throw new IllegalArgumentException("game event field must be boolean: " + name);
        return Optional.of(n.booleanValue());
    }
    private static Optional<JsonNode> optionalNode(JsonNode n) { return n == null || n.isNull() ? Optional.empty() : Optional.of(n); }
    private static Optional<CardIdentity> card(JsonNode n) {
        if (n == null || n.isNull()) return Optional.empty();
        if (!n.isObject()) throw new IllegalArgumentException("card identity must be an object");
        JsonNode suit = n.get("suitIndex"), rank = n.get("rank");
        if (suit == null && rank == null && n.isEmpty()) return Optional.empty();
        if (suit == null || rank == null || !suit.isIntegralNumber() || !rank.isIntegralNumber() || !suit.canConvertToInt() || !rank.canConvertToInt() || suit.intValue() < 0 || suit.intValue() > 5 || rank.intValue() < 1 || rank.intValue() > 7)
            throw new IllegalArgumentException("invalid card identity");
        return Optional.of(new CardIdentity(suit.intValue(), rank.intValue()));
    }
}
