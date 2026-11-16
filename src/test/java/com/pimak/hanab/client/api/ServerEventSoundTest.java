package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventSoundTest {
    @Test void decodesKnownLobbySounds() throws Exception {
        var json = new ObjectMapper().readTree("{\"sound\":\"someoneJoined\"}");
        var event = new ServerEvent(Instant.now(), new WireMessage("lobbySound", json));
        assertEquals(LobbySound.SOMEONE_JOINED, event.lobbySound().orElseThrow());
    }

    @Test void preservesUnknownSoundNames() {
        var event = new ServerEvent(java.time.Instant.now(), new com.pimak.hanab.client.protocol.WireMessage(
                "lobbySound", new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("sound", "futureSound")));
        assertEquals("futureSound", event.lobbySoundName().orElseThrow());
        assertThrows(IllegalArgumentException.class, event::lobbySound);
    }
    @Test void exposesChatSoundWithoutChoosingAnAudioEngine() {
        var event = new ServerEvent(Instant.now(), new WireMessage("chatSound",
                new ObjectMapper().createObjectNode().put("sound", "message")));
        assertEquals("message", event.chatSoundName().orElseThrow());
    }
}
