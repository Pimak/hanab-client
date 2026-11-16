package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CharacterAssignmentTest {
    private final ObjectMapper json = new ObjectMapper();
    @Test void parsesAllAssignmentKinds() throws Exception {
        assertEquals(new CharacterAssignment.Plain(2), CharacterAssignment.from(json.readTree("{\"type\":\"plain\",\"characterId\":2}")));
        assertEquals(new CharacterAssignment.Color(2, 4), CharacterAssignment.from(json.readTree("{\"type\":\"color\",\"characterId\":2,\"color\":4}")));
        assertEquals(new CharacterAssignment.Rank(2, 5), CharacterAssignment.from(json.readTree("{\"type\":\"rank\",\"characterId\":2,\"rank\":5}")));
    }
    @Test void rejectsInvalidAssignment() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> CharacterAssignment.from(json.readTree("{\"type\":\"color\",\"characterId\":2,\"color\":6}")));
        assertThrows(IllegalArgumentException.class, () -> CharacterAssignment.from(json.readTree("{\"type\":\"plain\",\"characterId\":2,\"rank\":1}")));
    }
}
