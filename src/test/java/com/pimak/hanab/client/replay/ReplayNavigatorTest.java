package com.pimak.hanab.client.replay;

import com.pimak.hanab.client.model.Replay;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReplayNavigatorTest {
    private static Replay replay() {
        return new Replay("p2v0s1", List.of("Alice", "Bob"), List.of(
                Map.of("type", "play", "order", 4),
                Map.of("type", "clue", "target", 1),
                Map.of("type", "discard", "order", 2)),
                Map.of("deck", List.of(Map.of("suitIndex", 0, "rank", 1)), "options", Map.of("deckPlays", true)));
    }

    @Test
    void navigationIsImmutableAndStartsWithoutFutureActions() {
        var initial = ReplayNavigator.of(replay());
        assertEquals(0, initial.turn());
        assertTrue(initial.visibleActions().isEmpty());
        assertTrue(initial.next().next().visibleActions().contains(Map.of("type", "clue", "target", 1)));
        assertTrue(initial.visibleActions().isEmpty());
        assertEquals(3, initial.last().turn());
        assertEquals(0, initial.last().previous().previous().previous().turn());
    }

    @Test
    void exportContainsOnlyTheReachedPrefixAndPreservesHanabMetadata() throws Exception {
        var json = ReplayNavigator.of(replay()).atTurn(1).exportJson();
        assertTrue(json.contains("\"seed\":\"p2v0s1\""));
        assertTrue(json.contains("\"deck\""));
        assertTrue(json.contains("\"type\":\"play\""));
        assertFalse(json.contains("\"type\":\"clue\""));
        assertFalse(ReplayNavigator.of(replay()).atTurn(0).exportSpoilerFreeJson().contains("\"deck\""));

        var imported = ReplayNavigator.importJson(json);
        assertEquals(1, imported.last().turn());
        var action = imported.last().currentAction().orElseThrow();
        assertEquals("play", action.get("type"));
        assertEquals(4, ((Number) action.get("order")).intValue());
    }

    @Test
    void nestedInputValuesAreDefensivelyCopied() {
        var nested = new java.util.ArrayList<>(List.of("before"));
        var input = new Replay("seed", List.of("Alice"), List.of(Map.of("type", "note", "values", nested)));
        var navigator = ReplayNavigator.of(input);
        nested.set(0, "after");
        assertEquals(List.of("before"), navigator.last().currentAction().orElseThrow().get("values"));
    }

    @Test
    void invalidTurnsAreRejected() {
        var navigator = ReplayNavigator.of(replay());
        assertThrows(IllegalArgumentException.class, () -> navigator.atTurn(-1));
        assertThrows(IllegalArgumentException.class, () -> navigator.atTurn(4));
    }

    @Test
    void importsSeedOnlyDocuments() throws Exception {
        var navigator = ReplayNavigator.importJson("{\"seed\":\"abc\"}");
        assertEquals(0, navigator.turn());
    }

    @Test
    void visibleReplayDoesNotExposeFutureMetadata() {
        var visible = ReplayNavigator.of(replay()).atTurn(0).visibleReplay();
        assertTrue(visible.seed().isEmpty());
        assertTrue(visible.actions().isEmpty());
        assertTrue(visible.unknownFields().isEmpty());
    }
}
