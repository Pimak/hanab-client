package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IdentitySnapshotTest {
    @Test void decodesTypedIdentityAndRetainsRawFields() throws Exception {
        var value = IdentitySnapshot.from(new ObjectMapper().readTree("{\"userId\":7,\"username\":\"bot\",\"status\":\"online\",\"friends\":[\"alice\"],\"future\":true}"));
        assertEquals(7, value.userId().orElseThrow().value());
        assertEquals(java.util.Set.of("alice"), value.friends());
        assertTrue(value.raw().path("future").asBoolean());
    }
}
