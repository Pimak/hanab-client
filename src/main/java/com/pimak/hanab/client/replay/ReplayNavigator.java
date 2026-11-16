package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.model.Replay;
import com.pimak.hanab.client.model.ReplayDocument;
import com.pimak.hanab.client.game.GameEvent;
import com.pimak.hanab.client.game.GameState;
import com.pimak.hanab.client.game.GameStateReducer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pimak.hanab.client.protocol.ProtocolDecodeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An immutable, perspective-safe cursor over a Hanab Live replay.
 *
 * <p>A cursor exposes only the actions up to its current turn. In particular, merely holding a
 * cursor at turn zero does not expose the action list that will happen later. Navigation returns
 * another cursor and never changes this instance.</p>
 */
public final class ReplayNavigator {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Replay replay;
    private final int turn;

    /** Creates a spoiler-free cursor positioned before the first action. */
    public ReplayNavigator(Replay replay) {
        this(replay, 0);
    }

    private ReplayNavigator(Replay replay, int turn) {
        this.replay = copyReplay(Objects.requireNonNull(replay, "replay"));
        if (turn < 0 || turn > this.replay.actions().size()) {
            throw new IllegalArgumentException("turn must be between 0 and the replay length");
        }
        this.turn = turn;
    }

    public static ReplayNavigator of(Replay replay) {
        return new ReplayNavigator(replay);
    }

    /** Imports the Hanab Live replay JSON using the existing protocol replay decoder. */
    public static ReplayNavigator importJson(String json) throws ProtocolDecodeException {
        ReplayDocument document = ReplayDocumentCodec.importJson(Objects.requireNonNull(json, "json"));
        var unknown = new LinkedHashMap<String, Object>(document.unknownFields());
        if (!document.deck().isEmpty()) unknown.put("deck", document.deck());
        if (!document.options().isEmpty()) unknown.put("options", document.options());
        if (!document.notes().isEmpty()) unknown.put("notes", document.notes());
        if (!document.characters().isEmpty()) unknown.put("characters", document.characters());
        return new ReplayNavigator(new Replay(document.seed(), document.playerNames(), document.actions(), unknown));
    }

    /** Alias for callers that prefer a factory-style name. */
    public static ReplayNavigator fromJson(String json) throws ProtocolDecodeException {
        return importJson(json);
    }

    /** Returns a cursor before the first action. */
    public ReplayNavigator first() {
        return turn == 0 ? this : new ReplayNavigator(replay, 0);
    }

    /** Returns a cursor after the last action. This is the explicit opt-in to the full replay. */
    public ReplayNavigator last() {
        return turn == replay.actions().size() ? this : new ReplayNavigator(replay, replay.actions().size());
    }

    /** Advances one action, or returns this cursor when already at the end. */
    public ReplayNavigator next() {
        return turn == replay.actions().size() ? this : new ReplayNavigator(replay, turn + 1);
    }

    /** Goes back one action, or returns this cursor when already at the beginning. */
    public ReplayNavigator previous() {
        return turn == 0 ? this : new ReplayNavigator(replay, turn - 1);
    }

    /** Positions the cursor at a boundary: zero means before the first action. */
    public ReplayNavigator atTurn(int requestedTurn) {
        return requestedTurn == turn ? this : new ReplayNavigator(replay, requestedTurn);
    }

    public int turn() {
        return turn;
    }

    public boolean hasNext() {
        return turn < replay.actions().size();
    }

    public boolean hasPrevious() {
        return turn > 0;
    }

    /** The actions visible at this cursor; future actions are not present in this list. */
    public List<Map<String, Object>> visibleActions() {
        return replay.actions().subList(0, turn).stream().map(ReplayNavigator::copyMap).toList();
    }

    /** The action most recently reached by this cursor, if any. */
    public Optional<Map<String, Object>> currentAction() {
        return turn == 0 ? Optional.empty() : Optional.of(copyMap(replay.actions().get(turn - 1)));
    }

    /**
     * Decodes the visible prefix as the server's game-event union. This method deliberately
     * rejects replay collaboration actions such as arrows or sounds; callers must handle those
     * separately instead of silently treating them as game events.
     */
    public List<GameEvent> visibleGameEvents() {
        var events = new ArrayList<GameEvent>();
        int currentPlayer = 0;
        for (Map<String, Object> action : replay.actions().subList(0, turn)) {
            JsonNode normalized = normalizeAction(action, currentPlayer);
            GameEvent event = GameEvent.parse(normalized);
            events.add(event);
            if (event instanceof GameEvent.TurnEvent next && next.currentPlayerIndex().isPresent()) currentPlayer = next.currentPlayerIndex().get();
            else if (event instanceof GameEvent.CardEvent || event instanceof GameEvent.ClueEvent) currentPlayer = nextPlayer(currentPlayer);
        }
        return List.copyOf(events);
    }

    /** Replays the visible event prefix from a supplied perspective-safe initial state. */
    public GameState stateAtTurn(GameState initial) {
        Objects.requireNonNull(initial, "initial");
        var reducer = new GameStateReducer();
        GameState state = initial;
        int currentPlayer = 0;
        for (Map<String, Object> action : replay.actions().subList(0, turn)) {
            JsonNode event = normalizeAction(action, currentPlayer);
            ObjectNode envelope = JSON.createObjectNode();
            envelope.set("event", event);
            state = reducer.reduce(state, envelope);
            GameEvent parsed = GameEvent.parse(event);
            if (parsed instanceof GameEvent.TurnEvent next && next.currentPlayerIndex().isPresent()) currentPlayer = next.currentPlayerIndex().get();
            else if (parsed instanceof GameEvent.CardEvent || parsed instanceof GameEvent.ClueEvent) currentPlayer = nextPlayer(currentPlayer);
        }
        return state;
    }

    private static JsonNode normalizeAction(Map<String, Object> action, int currentPlayer) {
        JsonNode raw = JSON.valueToTree(action);
        if (!raw.isObject() || !raw.path("type").isTextual()) throw new IllegalArgumentException("replay action must have a type");
        String type = raw.path("type").textValue();
        if (raw.has("playerIndex") || raw.has("receiverIndex") || raw.has("endCondition") || type.equals("turn") || type.equals("playerTimes")) return raw;
        ObjectNode event = raw.deepCopy();
        switch (type) {
            case "play", "discard", "misplay", "draw", "cardIdentity" -> event.put("playerIndex", currentPlayer);
            case "clue" -> {
                JsonNode target = raw.get("target");
                if (target == null || !target.isIntegralNumber() || !target.canConvertToInt()) throw new IllegalArgumentException("compact clue action is missing target");
                event.put("playerIndex", currentPlayer).put("receiverIndex", target.intValue());
                if (!event.has("orders")) event.putArray("orders");
                event.remove("target");
            }
            default -> throw new IllegalArgumentException("replay collaboration action is not a game event: " + type);
        }
        return event;
    }

    private int nextPlayer(int currentPlayer) {
        return replay.playerNames().isEmpty() ? currentPlayer : (currentPlayer + 1) % replay.playerNames().size();
    }

    /** A replay value containing only the visible prefix and the original metadata. */
    public Replay visibleReplay() {
        return new Replay(java.util.Optional.empty(), replay.playerNames(), visibleActions(), Map.of());
    }

    /** Exports only the visible prefix in Hanab Live JSON format. */
    public String exportJson() {
        return exportJson(false);
    }

    /** Exports a prefix without deck/character identities that could reveal future information. */
    public String exportSpoilerFreeJson() {
        return exportJson(true);
    }

    private String exportJson(boolean spoilerFree) {
        var root = new LinkedHashMap<String, Object>();
        replay.seed().ifPresent(seed -> root.put("seed", seed));
        root.put("playerNames", replay.playerNames());
        root.put("actions", visibleActions());
        if (!spoilerFree) replay.unknownFields().forEach(root::putIfAbsent);
        if (spoilerFree) root.remove("seed");
        try {
            return JSON.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("unable to encode replay JSON", e);
        }
    }

    private static Replay copyReplay(Replay source) {
        var actions = source.actions().stream().map(ReplayNavigator::copyMap).toList();
        return new Replay(source.seed(), source.playerNames(), actions, copyMap(source.unknownFields()));
    }

    private static Map<String, Object> copyMap(Map<String, ?> source) {
        var result = new LinkedHashMap<String, Object>();
        source.forEach((key, value) -> result.put(key, copyValue(value)));
        return Collections.unmodifiableMap(result);
    }

    private static Object copyValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<String, Object>();
            map.forEach((key, child) -> result.put(String.valueOf(key), copyValue(child)));
            return Collections.unmodifiableMap(result);
        }
        if (value instanceof List<?> list) {
            var result = new ArrayList<Object>(list.size());
            list.forEach(child -> result.add(copyValue(child)));
            return List.copyOf(result);
        }
        if (value instanceof JsonNode node) return node.deepCopy();
        return value;
    }
}
