package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameEventValidationTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void rejectsMalformedRequiredArraysAndBooleans() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"clue\",\"playerIndex\":0,\"receiverIndex\":1,\"orders\":\"4\"}")));
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"clue\",\"playerIndex\":0,\"receiverIndex\":1,\"orders\":[4],\"ignoreNegative\":\"false\"}")));
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"playerTimes\",\"durationMs\":1,\"playerTimesMs\":null}")));
    }

    @Test void rejectsNegativeAndMalformedNumericFields() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"draw\",\"playerIndex\":-1,\"order\":2}")));
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"playerTimes\",\"durationMs\":-1,\"playerTimesMs\":[]}")));
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"play\",\"playerIndex\":0,\"order\":2,\"identity\":{\"rank\":9}}")));
    }

    @Test void retainsAnExplicitlyHiddenCardIdentity() throws Exception {
        var event = GameEvent.parse(mapper.readTree(
                "{\"type\":\"draw\",\"playerIndex\":0,\"order\":2,\"identity\":{}}"));
        assert event instanceof GameEvent.CardEvent card && card.identity().isEmpty();
    }

    @Test void acceptsAValidStackStartCardAndNegativeOvertime() throws Exception {
        var card = GameEvent.parse(mapper.readTree(
                "{\"type\":\"draw\",\"playerIndex\":0,\"order\":0,\"identity\":{\"suitIndex\":0,\"rank\":7}}"));
        assertEquals(7, ((GameEvent.CardEvent) card).identity().orElseThrow().rank());
        var times = GameEvent.parse(mapper.readTree(
                "{\"type\":\"playerTimes\",\"durationMs\":500,\"playerTimesMs\":[1000,-2000]}"));
        assertEquals(-2000L, ((GameEvent.PlayerTimesEvent) times).playerTimesMs().get(1));
    }

    @Test void rejectsClueOrderOutsideTheDeck() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> GameEvent.parse(mapper.readTree(
                "{\"type\":\"clue\",\"playerIndex\":0,\"receiverIndex\":1,\"orders\":[66]}")));
    }
}
