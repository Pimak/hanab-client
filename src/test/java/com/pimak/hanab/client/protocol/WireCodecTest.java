package com.pimak.hanab.client.protocol;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class WireCodecTest {
    @Test void encodesCurrentBotRequestFrame() throws Exception {
        var codec = new WireCodec();
        var frame = codec.encodeRequest(12, "tableJoin", new ObjectMapper().createObjectNode().put("tableId", 42));
        assertEquals("{\"id\":12,\"request\":\"tableJoin\",\"data\":{\"tableId\":42}}", frame);
    }

    @Test void decodesEventAndReplyFrames() throws Exception {
        var codec = new WireCodec();
        var event = codec.decodeFrame("{\"event\":\"lobbyStatus\",\"data\":{\"loading\":false}}");
        assertEquals(WireFrame.Kind.EVENT, event.kind());
        assertEquals("lobbyStatus", event.name());
        var reply = codec.decodeFrame("{\"reply\":12,\"data\":{}}");
        assertEquals(WireFrame.Kind.REPLY, reply.kind());
        assertEquals(12, reply.id());
    }

    @Test void rejectsLegacyTextFramesAndMalformedStructuredFrames() {
        var codec = new WireCodec();
        assertThrows(Exception.class, () -> codec.decodeFrame("tableJoin {\"tableId\":12}"));
        assertThrows(Exception.class, () -> codec.decodeFrame("{\"reply\":0,\"data\":{}}"));
        assertThrows(Exception.class, () -> codec.decodeFrame("{\"event\":\"chat\",\"data\":null}"));
    }
}
