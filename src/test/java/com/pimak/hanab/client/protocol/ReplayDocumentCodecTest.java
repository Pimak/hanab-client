package com.pimak.hanab.client.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplayDocumentCodecTest {
    @Test
    void acceptsSeedOnlyReplayAndPreservesOptionalMetadata() throws Exception {
        var replay = ProtocolPayloadCodec.decodeReplayDocument("""
                {"seed":"p2v0s1","options":{"speedrun":true},"actions":[{"type":"play","order":3}],"future":true}
                """);
        assertEquals("p2v0s1", replay.seed().orElseThrow());
        assertEquals(true, replay.options().get("speedrun"));
        assertEquals("play", replay.actions().get(0).get("type"));
        assertTrue(replay.unknownFields().containsKey("future"));
    }

    @Test
    void legacyReplayDecoderAcceptsDeckOnlyDocuments() throws Exception {
        var replay = ProtocolPayloadCodec.decodeReplay("{\"deck\":[{\"suitIndex\":0,\"rank\":1}],\"actions\":[]}");
        assertTrue(replay.seed().isEmpty());
        assertTrue(replay.unknownFields().containsKey("deck"));
    }
}
