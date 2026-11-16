package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SharedReplaySessionDecodeTest {
    @Test void decodesProtocolSharedReplayState() throws Exception {
        var mode = new ObjectMapper().readTree("{\"type\":\"sharedReplay\",\"sharedReplay\":{\"owner\":{\"userId\":1,\"username\":\"Alice\"},\"turn\":3,\"efficiencyModifier\":-2,\"spoilerFree\":true,\"restartDeclined\":[\"Bob\"],\"canPlayAgain\":false,\"replayStartedAt\":\"2026-01-01T00:00:00.000Z\"}}");
        var session = SharedReplaySession.fromMode(mode);
        assertEquals("Alice", session.owner());
        assertEquals(-2, session.efficiencyModifier().orElseThrow());
        assertEquals(java.util.List.of("Bob"), session.restartDeclined());
    }
}
