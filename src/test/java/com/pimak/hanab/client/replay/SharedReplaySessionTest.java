package com.pimak.hanab.client.replay;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

class SharedReplaySessionTest {
    @Test void collaborationProjectionIsImmutable() {
        var state = new SharedReplaySession("Alice", 3, true, true);
        var next = state.atTurn(4).withAnnotation(new SharedReplaySession.ReplayAnnotation("arrow", JsonNodeFactory.instance.objectNode()));
        assertEquals(3, state.turn());
        assertEquals(4, next.turn());
        assertEquals(1, next.annotations().size());
    }

    @Test void decodesAnnotationsAndRejectsOutOfRangeTurns() throws Exception {
        var mode = new ObjectMapper().readTree("{\"type\":\"sharedReplay\",\"sharedReplay\":{\"owner\":{\"userId\":1,\"username\":\"Alice\"},\"turn\":3,\"efficiencyModifier\":0,\"spoilerFree\":true,\"canPlayAgain\":true,\"restartDeclined\":[],\"replayStartedAt\":\"2026-01-01T00:00:00.000Z\",\"annotations\":[{\"kind\":\"arrow\",\"value\":{\"from\":1}}]}}");
        var state = SharedReplaySession.fromMode(mode);
        assertEquals(1, state.annotations().size());
        assertThrows(IllegalArgumentException.class, () -> state.atTurn(513));
    }
}
