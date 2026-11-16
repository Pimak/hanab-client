package com.pimak.hanab.client.api;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class ServerEventReplayLifecycleTest {
    @Test void decodesSuggestionsAndDeclines() throws Exception {
        var mapper=new ObjectMapper();
        var suggestion=new ServerEvent(Instant.now(),new WireMessage("turnSuggestion",mapper.readTree("{\"tableId\":7,\"username\":\"Alice\",\"turn\":3}")));
        assertEquals(3,suggestion.turnSuggestion().orElseThrow().turn());
        var declined=new ServerEvent(Instant.now(),new WireMessage("restartDeclined",mapper.readTree("{\"tableId\":7,\"restartDeclined\":[\"Alice\"]}")));
        assertEquals(List.of("Alice"),declined.restartDeclined().orElseThrow().usernames());
        var finished=new ServerEvent(Instant.now(),new WireMessage("gameFinished",mapper.readTree("{\"tableId\":7,\"gameId\":42,\"sharedReplay\":{\"owner\":{\"userId\":1,\"username\":\"Alice\"}}}")));
        assertEquals(42, finished.gameFinished().orElseThrow().gameId().orElseThrow().value());
    }
}
