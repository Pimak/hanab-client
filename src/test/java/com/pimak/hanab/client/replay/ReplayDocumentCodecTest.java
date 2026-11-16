package com.pimak.hanab.client.replay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayDocumentCodecTest {
    @Test void roundTripsOptionalReplayFields() throws Exception {
        var source = "{\"seed\":\"abc\",\"playerNames\":[\"Alice\"],\"actions\":[{\"type\":\"play\",\"order\":0}],\"notes\":[[\"keep\"]],\"future\":true}";
        var document = ReplayDocumentCodec.importJson(source);
        var exported = ReplayDocumentCodec.exportJson(document);
        assertTrue(exported.contains("\"future\":true"));
        assertEquals("abc", ReplayDocumentCodec.importJson(exported).seed().orElseThrow());
    }
}
