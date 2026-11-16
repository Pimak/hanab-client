package com.pimak.hanab.client.protocol;

import com.pimak.hanab.client.game.GameStateReducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolFixtureTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void modernGameFixturesKeepUnknownFieldsAndVisibleIdentities() throws Exception {
        var reducer = new GameStateReducer();
        var init = read("game-init.json");
        var state = reducer.reduce(init);
        state = reducer.reduce(state, read("game-state.json"));
        state = reducer.reduce(state, read("game-event.json"));
        assertEquals(3, state.cards().get(4).rank());
        assertEquals(42, state.rawState().path("futureState").asInt());
        assertEquals("kept", state.events().get(1).raw().path("futureEvent").asText());
    }
    private com.fasterxml.jackson.databind.JsonNode read(String name) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/protocol/" + name)) { assertNotNull(stream); return json.readTree(stream); }
    }
}
