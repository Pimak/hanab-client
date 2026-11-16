package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventRelationsTest {
    @Test void decodesFriendsAndBlockedLists() {
        var friends = JsonNodeFactory.instance.objectNode().set("friends", JsonNodeFactory.instance.arrayNode().add("Alice").add("Bob"));
        var blocked = JsonNodeFactory.instance.objectNode().set("blocked", JsonNodeFactory.instance.arrayNode().add("Mallory"));
        assertEquals(List.of("Alice", "Bob"), new ServerEvent(Instant.now(), new WireMessage("friends", friends)).friends().orElseThrow());
        assertEquals(List.of("Mallory"), new ServerEvent(Instant.now(), new WireMessage("blocked", blocked)).blocked().orElseThrow());
    }
}
