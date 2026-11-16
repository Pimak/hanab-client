package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.LobbyStatus;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventLobbyStatusTest {
    @Test void decodesLobbyStatuses() {
        var loading = new ServerEvent(Instant.now(), new WireMessage("lobbyStatus", com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("type", "loading")));
        var ready = new ServerEvent(Instant.now(), new WireMessage("lobbyStatus", com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode().put("type", "ready").put("numGames", 12)));
        assertInstanceOf(LobbyStatus.Loading.class, loading.lobbyStatus().orElseThrow());
        assertEquals(12, ((LobbyStatus.Ready) ready.lobbyStatus().orElseThrow()).numGames());
    }
}
