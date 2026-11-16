package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Applies server snapshots and events mechanically, retaining the recipient's perspective. */
public final class GameStateReducer {
    public GameState reduce(JsonNode message) { return reduce(GameState.empty(), message); }

    public GameState reduce(GameState previous, JsonNode message) {
        if (previous == null || message == null || !message.isObject()) throw new IllegalArgumentException("message must be an object");
        if (message.has("event") && message.get("event").isObject()) return apply(previous, GameEvent.parse(message.get("event")));
        if (message.has("event") && !message.get("event").isObject()) throw new IllegalArgumentException("event must be an object");
        if (message.has("events")) {
            if (!message.get("events").isArray()) throw new IllegalArgumentException("events must be an array");
            return snapshot(previous, message);
        }
        return init(previous, message);
    }

    public GameState apply(GameState state, GameEvent event) {
        var events = new ArrayList<>(state.events()); events.add(event);
        return rebuild(state, events, state.visibleState(), state.rawState(), state.cards());
    }

    private GameState init(GameState old, JsonNode init) {
        String mode = mode(init.get("mode"));
        List<String> playerNames = strings(init.get("playerNames"));
        return new GameState(optionalLong(init.get("tableId")), mode, playerNames,
                emptyHands(playerNames.size()), Map.of(), List.of(), List.of(), List.of(), List.of(), Optional.empty(), Optional.empty(), Optional.empty(),
                List.of(), List.of(), init, old.rawState(), init);
    }

    private GameState snapshot(GameState old, JsonNode state) {
        var parsed = new ArrayList<GameEvent>();
        state.get("events").forEach(n -> parsed.add(GameEvent.parse(n)));
        var cards = new LinkedHashMap<Integer, CardIdentity>(old.cards());
        JsonNode deck = state.get("deck");
        if (deck != null && !deck.isNull() && !deck.isArray()) throw new IllegalArgumentException("deck must be an array");
        if (deck != null && deck.isArray()) for (int i = 0; i < deck.size(); i++) {
            JsonNode c = deck.get(i);
            if (c == null || c.isNull()) continue;
            if (!c.isObject()) throw new IllegalArgumentException("deck card must be an object");
            {
                JsonNode suit = c.get("suitIndex"), rank = c.get("rank");
                if (suit == null && rank == null && c.isEmpty()) continue;
                if (suit != null && rank != null && suit.isIntegralNumber() && rank.isIntegralNumber()
                        && suit.canConvertToInt() && rank.canConvertToInt() && suit.intValue() >= 0 && suit.intValue() <= 5 && rank.intValue() >= 1 && rank.intValue() <= 7) {
                    cards.put(i, new CardIdentity(suit.intValue(), rank.intValue()));
                } else throw new IllegalArgumentException("invalid deck card identity");
            }
        }
        var table = optionalLong(state.get("tableId")).or(() -> old.tableId());
        var visible = state.deepCopy();
        return rebuild(new GameState(table, old.mode(), old.playerNames(), old.hands(), old.cards(), old.clueOrders(), old.discardOrders(), old.playOrders(), old.misplayOrders(), old.turn(), old.currentPlayerIndex(), old.gameOver(), old.playerTimesMs(), List.of(), visible, old.rawInit(), state), parsed, visible, state, cards);
    }

    private GameState rebuild(GameState base, List<GameEvent> events, JsonNode visible, JsonNode rawState, Map<Integer, CardIdentity> initialCards) {
        var hands = new ArrayList<List<Integer>>(); for (int i = 0; i < base.playerNames().size(); i++) hands.add(new ArrayList<>());
        var cards = new LinkedHashMap<>(initialCards); var clues = new ArrayList<Integer>(); var discards = new ArrayList<Integer>();
        var plays = new ArrayList<Integer>(); var misplays = new ArrayList<Integer>(); Optional<Integer> turn = Optional.empty(), current = Optional.empty();
        Optional<GameEvent.GameOverEvent> over = Optional.empty(); List<Long> times = List.of();
        for (GameEvent event : events) {
            if (event instanceof GameEvent.CardEvent card) {
                card.identity().ifPresent(identity -> cards.put(card.order(), identity));
                if (card.isDraw()) add(hands, card.playerIndex(), card.order());
                if (card.isPlay()) { remove(hands, card.playerIndex(), card.order()); plays.add(card.order()); }
                if (card.isDiscard()) { remove(hands, card.playerIndex(), card.order()); discards.add(card.order()); }
                if (card.isMisplay()) { remove(hands, card.playerIndex(), card.order()); misplays.add(card.order()); }
            } else if (event instanceof GameEvent.ClueEvent clue) clues.addAll(clue.orders());
            else if (event instanceof GameEvent.TurnEvent next) { turn = Optional.of(next.turn()); current = next.currentPlayerIndex(); }
            else if (event instanceof GameEvent.GameOverEvent end) over = Optional.of(end);
            else if (event instanceof GameEvent.PlayerTimesEvent timing) times = timing.playerTimesMs();
        }
        return new GameState(base.tableId(), base.mode(), base.playerNames(), hands, cards, clues, discards, plays, misplays,
                turn, current, over, times, events, visible, base.rawInit(), rawState);
    }

    private static void add(List<List<Integer>> hands, int player, int order) { if (player >= 0 && player < hands.size()) hands.get(player).add(order); }
    private static void remove(List<List<Integer>> hands, int player, int order) { if (player >= 0 && player < hands.size()) hands.get(player).remove((Integer) order); }
    private static List<List<Integer>> emptyHands(int n) { var result = new ArrayList<List<Integer>>(); for (int i = 0; i < n; i++) result.add(List.of()); return result; }
    private static String mode(JsonNode n) {
        if (n == null || n.isNull()) return "unknown";
        if (n.isTextual() && !n.asText().isBlank()) return n.asText();
        if (n.isObject() && n.has("type") && n.get("type").isTextual() && !n.get("type").asText().isBlank()) return n.get("type").asText();
        throw new IllegalArgumentException("mode must be text or an object with textual type");
    }
    private static List<String> strings(JsonNode n) {
        if (n == null || n.isNull()) return List.of();
        if (!n.isArray()) throw new IllegalArgumentException("playerNames must be an array");
        var result = new ArrayList<String>();
        n.forEach(v -> { if (!v.isTextual() || v.asText().isBlank()) throw new IllegalArgumentException("playerNames must contain text"); result.add(v.asText()); });
        return result;
    }
    private static Optional<Long> optionalLong(JsonNode n) {
        if (n == null || n.isNull()) return Optional.empty();
        if (!n.isIntegralNumber() || !n.canConvertToLong() || n.longValue() < 0) throw new IllegalArgumentException("tableId must be a non-negative long");
        return Optional.of(n.longValue());
    }
}
