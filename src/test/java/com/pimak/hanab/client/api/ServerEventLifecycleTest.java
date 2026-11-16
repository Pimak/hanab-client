package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerEventLifecycleTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void projectsTableStartOwnerSpectatorsPauseAndVotes() throws Exception {
        BiFunction<String, String, ServerEvent> event = (command, json) -> {
            try { return new ServerEvent(Instant.EPOCH, new WireMessage(command, mapper.readTree(json))); }
            catch (Exception error) { throw new AssertionError(error); }
        };
        assertEquals(TableStarted.Stage.PLAYING, event.apply("tableStarted", "{\"tableId\":3,\"stage\":\"playing\"}").tableStarted().orElseThrow().stage());
        assertEquals("Alice", event.apply("tableOwner", "{\"tableId\":3,\"owner\":{\"userId\":7,\"username\":\"Alice\"}}").tableOwnerChanged().orElseThrow().owner().username());
        assertEquals(1, event.apply("spectators", "{\"tableId\":3,\"spectators\":[{\"userId\":9,\"username\":\"Carol\"}]}").spectatorsUpdated().orElseThrow().spectators().size());
        assertEquals(1, event.apply("pause", "{\"tableId\":3,\"pausingPlayerIndex\":1,\"queued\":true}").pauseState().orElseThrow().pausingPlayerIndex().orElseThrow());
        assertEquals(2, event.apply("votes", "{\"tableId\":3,\"voted\":false,\"numVotesNeeded\":2}").votes().orElseThrow().votesNeeded());
    }
}
