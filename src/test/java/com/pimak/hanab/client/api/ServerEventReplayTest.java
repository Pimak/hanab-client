package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventReplayTest {
    @Test void preservesTableIdForGameAndReplayEvents() throws Exception {
        var mapper = new ObjectMapper();
        var game = mapper.readTree("{\"tableId\":7,\"event\":{\"type\":\"turn\",\"turn\":2}}");
        var replay = mapper.readTree("{\"tableId\":7,\"action\":{\"type\":\"turn\",\"turn\":2}}");
        assertEquals(7, new ServerEvent(Instant.now(), new WireMessage("gameEvent", game)).gameEventMessage().orElseThrow().tableId().value());
        assertEquals(7, new ServerEvent(Instant.now(), new WireMessage("replayAction", replay)).replayAction().orElseThrow().tableId().value());
        assertEquals(2, ((com.pimak.hanab.client.replay.ReplayAction.Turn)
                new ServerEvent(Instant.now(), new WireMessage("replayAction", replay)).replayAction().orElseThrow().action()).turn());
        assertEquals(0, new com.pimak.hanab.client.model.Ids.TableId(0).value());
        assertEquals(9_007_199_254_740_991L, new com.pimak.hanab.client.model.Ids.TableId(9_007_199_254_740_991L).value());
        assertThrows(IllegalArgumentException.class, () -> new com.pimak.hanab.client.model.Ids.TableId(9_007_199_254_740_992L));
    }
}
