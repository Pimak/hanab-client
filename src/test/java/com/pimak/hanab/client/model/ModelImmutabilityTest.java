package com.pimak.hanab.client.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class ModelImmutabilityTest {
    @Test
    void valuesAreImmutableAndValidateRequiredData() {
        var user = new User(7, "Alice");
        assertThrows(UnsupportedOperationException.class,
                () -> user.unknownFields().put("future", true));
        assertEquals("Alice", user.username());
        assertThrows(IllegalArgumentException.class, () -> new User(7, ""));
    }

    @Test
    void nestedValuesAreCopied() {
        var events = new java.util.ArrayList<String>();
        events.add("draw");
        var replay = new Replay("p2v0s1", List.of("Alice", "Bob"), List.of(java.util.Map.of("type", "play")));
        events.add("play");
        assertEquals(List.of("Alice", "Bob"), replay.playerNames());
        assertThrows(UnsupportedOperationException.class, () -> replay.playerNames().add("Carol"));
    }
}
