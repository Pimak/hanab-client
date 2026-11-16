package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class GameStateReducerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final GameStateReducer reducer = new GameStateReducer();

    @Test void reducesVisibleEventsWithoutGuessingHiddenCards() throws Exception {
        var init = json.readTree("{\"tableId\":7,\"mode\":\"player\",\"playerNames\":[\"A\",\"B\"],\"futureOption\":true}");
        var state = reducer.reduce(init);
        var snapshot = json.readTree("{\"tableId\":7,\"events\":["
                + "{\"type\":\"draw\",\"playerIndex\":0,\"order\":4},"
                + "{\"type\":\"clue\",\"playerIndex\":1,\"receiverIndex\":0,\"orders\":[4],\"future\":{\"x\":1}},"
                + "{\"type\":\"play\",\"playerIndex\":0,\"order\":4,\"identity\":{\"suitIndex\":2,\"rank\":3}},"
                + "{\"type\":\"turn\",\"turn\":1,\"currentPlayerIndex\":1}],\"newField\":\"kept\"}");

        var result = reducer.reduce(state, snapshot);
        assertEquals(List.of(), result.hands().get(0));
        assertEquals(new CardIdentity(2, 3), result.cards().get(4));
        assertEquals(List.of(4), result.clueOrders());
        assertEquals(Optional.of(1), result.turn());
        assertEquals(Optional.of(1), result.currentPlayerIndex());
        assertEquals("kept", result.rawState().get("newField").asText());
        assertEquals(true, result.events().get(1).raw().get("future").get("x").asInt() == 1);
    }

    @Test void appliesIncrementalGameEventAndRetainsUnknownEvent() throws Exception {
        var state = reducer.reduce(json.readTree("{\"tableId\":9,\"mode\":\"spectator\",\"playerNames\":[\"A\",\"B\"]}"));
        state = reducer.reduce(state, json.readTree("{\"tableId\":9,\"event\":{\"type\":\"draw\",\"playerIndex\":1,\"order\":8}}"));
        state = reducer.reduce(state, json.readTree("{\"tableId\":9,\"event\":{\"type\":\"futureEvent\",\"answer\":42}}"));
        assertEquals(List.of(8), state.hands().get(1));
        assertInstanceOf(GameEvent.UnknownEvent.class, state.events().get(1));
        assertEquals(42, state.events().get(1).raw().get("answer").asInt());
    }

    @Test void timingAndGameOverAreTypedAndImmutable() throws Exception {
        var state = reducer.reduce(json.readTree("{\"tableId\":1,\"mode\":\"replay\",\"playerNames\":[\"A\",\"B\"]}"));
        state = reducer.reduce(state, json.readTree("{\"events\":[{\"type\":\"playerTimes\",\"durationMs\":12,\"playerTimesMs\":[100,200]},{\"type\":\"gameOver\",\"endCondition\":\"timeout\",\"votes\":[],\"playerIndex\":1}],\"score\":18}"));
        assertEquals(List.of(100L, 200L), state.playerTimesMs());
        assertEquals(18, state.score().orElseThrow());
        assertEquals("timeout", state.endCondition().orElseThrow());
        assertEquals("timeout", state.gameOver().orElseThrow().endCondition());
        var finalState = state;
        assertThrows(UnsupportedOperationException.class, () -> finalState.events().add(null));
    }
}
