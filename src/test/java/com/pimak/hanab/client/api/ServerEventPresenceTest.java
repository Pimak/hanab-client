package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventPresenceTest {
    @Test void decodesUserActivityAndRemovalById() throws Exception {
        var mapper = new ObjectMapper();
        var users = mapper.readTree("{\"users\":[{\"userId\":3,\"username\":\"Alice\",\"activity\":{\"type\":\"playing\",\"tableId\":7},\"hGroupConventions\":true,\"hGroupLevel\":4,\"bot\":true,\"inactive\":false}]}");
        var event = new ServerEvent(Instant.now(), new WireMessage("userList", users));
        assertEquals("playing", event.userList().orElseThrow().get(0).activity());
        var removed = new ServerEvent(Instant.now(), new WireMessage("userRemoved", mapper.readTree("{\"userId\":3}")));
        assertEquals(3, removed.userRemoved().orElseThrow());
    }
}
