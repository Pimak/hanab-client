package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/** Immutable projection of the events visible to one recipient. */
public record GameState(Optional<Long> tableId, String mode, List<String> playerNames,
                        List<List<Integer>> hands, Map<Integer, CardIdentity> cards,
                        List<Integer> clueOrders, List<Integer> discardOrders, List<Integer> playOrders,
                        List<Integer> misplayOrders, Optional<Integer> turn, Optional<Integer> currentPlayerIndex,
                        Optional<GameEvent.GameOverEvent> gameOver, List<Long> playerTimesMs,
                        List<GameEvent> events, JsonNode visibleState, JsonNode rawInit, JsonNode rawState) {
    public GameState {
        if (tableId == null || mode == null || playerNames == null || hands == null || cards == null || clueOrders == null
                || discardOrders == null || playOrders == null || misplayOrders == null || turn == null
                || currentPlayerIndex == null || gameOver == null || playerTimesMs == null || events == null
                || visibleState == null || rawInit == null || rawState == null) throw new NullPointerException();
        playerNames = List.copyOf(playerNames);
        hands = hands.stream().map(List::copyOf).toList();
        cards = Map.copyOf(cards);
        clueOrders = List.copyOf(clueOrders); discardOrders = List.copyOf(discardOrders);
        playOrders = List.copyOf(playOrders); misplayOrders = List.copyOf(misplayOrders);
        playerTimesMs = List.copyOf(playerTimesMs); events = List.copyOf(events);
        visibleState = visibleState.deepCopy(); rawInit = rawInit.deepCopy(); rawState = rawState.deepCopy();
    }
    @Override public JsonNode visibleState() { return visibleState.deepCopy(); }
    @Override public JsonNode rawInit() { return rawInit.deepCopy(); }
    @Override public JsonNode rawState() { return rawState.deepCopy(); }
    /** Current score when the server exposes it; absent before the initial state arrives. */
    public OptionalInt score() {
        JsonNode value = rawState.path("score").isNumber() ? rawState.path("score") : visibleState.path("score");
        return value.isNumber() ? OptionalInt.of(value.intValue()) : OptionalInt.empty();
    }
    /** Server end condition, if the game has finished. */
    public Optional<String> endCondition() { return gameOver.map(GameEvent.GameOverEvent::endCondition); }
    public boolean speedrunEnabled() { return gameOptions().map(com.pimak.hanab.client.model.GameOptions::speedrun).orElse(false); }
    public boolean speedrunFailed() { return endCondition().map("speedrunFail"::equals).orElse(false); }
    public java.util.Optional<com.pimak.hanab.client.replay.SharedReplaySession> sharedReplay() {
        JsonNode mode = rawInit.path("mode");
        if (!mode.isObject() || !"sharedReplay".equals(mode.path("type").asText())) return java.util.Optional.empty();
        return java.util.Optional.of(com.pimak.hanab.client.replay.SharedReplaySession.fromMode(mode));
    }
    public GameStateMetadata metadata() { return GameStateMetadata.from(visibleState, rawInit); }
    public java.util.Optional<com.pimak.hanab.client.model.GameOptions> gameOptions() {
        var value = rawInit.get("options");
        if (value == null) return java.util.Optional.empty();
        if (!value.isObject()) throw new IllegalArgumentException("options must be an object");
        return java.util.Optional.of(com.pimak.hanab.client.model.GameOptions.fromJson(value));
    }
    public List<CharacterAssignment> characterAssignments() {
        var value = rawInit.get("characters");
        if (value == null || value.isNull()) return List.of();
        if (!value.isArray()) throw new IllegalArgumentException("characters must be an array");
        if (value.size() > 6) throw new IllegalArgumentException("characters must contain at most six assignments");
        var result = new java.util.ArrayList<CharacterAssignment>(); value.forEach(item -> result.add(CharacterAssignment.from(item))); return List.copyOf(result);
    }
    public GameState withPatch(JsonNode patch) {
        if (patch == null || !patch.isObject()) throw new IllegalArgumentException("state patch must be an object");
        if (!visibleState.isObject() || !rawState.isObject()) throw new IllegalStateException("state projections must be objects");
        var nextVisible = (com.fasterxml.jackson.databind.node.ObjectNode) visibleState.deepCopy();
        var nextRaw = (com.fasterxml.jackson.databind.node.ObjectNode) rawState.deepCopy();
        patch.fields().forEachRemaining(entry -> { nextVisible.set(entry.getKey(), entry.getValue().deepCopy()); nextRaw.set(entry.getKey(), entry.getValue().deepCopy()); });
        return new GameState(tableId, mode, playerNames, hands, cards, clueOrders, discardOrders, playOrders, misplayOrders,
                turn, currentPlayerIndex, gameOver, playerTimesMs, events, nextVisible, rawInit, nextRaw);
    }
    public GameState withCardIdentities(JsonNode deck) {
        if (deck == null || !deck.isArray()) throw new IllegalArgumentException("deck must be an array");
        var nextCards = new java.util.LinkedHashMap<>(cards);
        for (int i = 0; i < deck.size(); i++) {
            var card = deck.get(i);
            if (card == null || card.isNull()) continue;
            if (!card.isObject()) throw new IllegalArgumentException("deck card must be an object");
            var suit = card.get("suitIndex"); var rank = card.get("rank");
            if (suit == null || rank == null || !suit.isIntegralNumber() || !rank.isIntegralNumber()
                    || !suit.canConvertToInt() || !rank.canConvertToInt() || suit.intValue() < 0 || suit.intValue() > 5
                    || rank.intValue() < 1 || rank.intValue() > 7) throw new IllegalArgumentException("invalid card identity");
            nextCards.put(i, new CardIdentity(suit.intValue(), rank.intValue()));
        }
        var patched = withPatch(com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().set("deck", deck.deepCopy()));
        return new GameState(patched.tableId(), patched.mode(), patched.playerNames(), patched.hands(), nextCards,
                patched.clueOrders(), patched.discardOrders(), patched.playOrders(), patched.misplayOrders(), patched.turn(),
                patched.currentPlayerIndex(), patched.gameOver(), patched.playerTimesMs(), patched.events(), patched.visibleState(), patched.rawInit(), patched.rawState());
    }
    public static GameState empty() { return new GameState(Optional.empty(), "unknown", List.of(), List.of(), Map.of(), List.of(), List.of(), List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), List.of(), com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(), com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(), com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode()); }
}
