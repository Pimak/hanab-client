package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameStateMetadataTest {
    @Test
    void exposesServerStateWithoutDroppingUnknownFields() throws Exception {
        var state = new GameStateReducer().reduce(new ObjectMapper().readTree("""
                {"tableId":1,"playerNames":["A","B"],"connected":[true,false],
                 "notes":[],"spectators":[],"votes":{"voted":true},"future":42}
                """));
        var metadata = state.metadata();
        assertEquals(java.util.List.of(true, false), metadata.connected());
        assertTrue(metadata.votes().isPresent());
        assertEquals(42, metadata.unknownFields().path("future").asInt());
    }
}
