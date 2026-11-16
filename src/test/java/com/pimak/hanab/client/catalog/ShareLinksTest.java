package com.pimak.hanab.client.catalog;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ShareLinksTest {
    @Test void roundTripsReplayReference() {
        var link = ShareLinks.replay(URI.create("https://hanab.live"), 42, OptionalInt.of(7));
        var reference = ShareLinks.parse(link);
        assertEquals(ShareLinks.Kind.REPLAY, reference.kind());
        assertEquals("42", reference.id());
        assertEquals(7, reference.turn().orElseThrow());
    }
}
