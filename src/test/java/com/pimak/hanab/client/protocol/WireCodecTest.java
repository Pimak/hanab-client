package com.pimak.hanab.client.protocol;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class WireCodecTest {
    @Test void encodesAndDecodesHanabTextFrame() throws Exception {
        var codec = new WireCodec();
        var decoded = codec.decode("tableJoin {\"tableId\":12}");
        assertEquals("tableJoin", decoded.command());
        assertEquals(12, decoded.payload().path("tableId").asInt());
        assertEquals("tableJoin {\"tableId\":12}", codec.encode(decoded));
    }

    @Test void acceptsEmptyJsonPayloadWhenOmitted() throws Exception {
        var decoded = new WireCodec().decode("sync");
        assertEquals("sync", decoded.command());
        assertTrue(decoded.payload().isObject());
    }
}
