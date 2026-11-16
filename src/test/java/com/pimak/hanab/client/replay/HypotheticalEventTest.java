package com.pimak.hanab.client.replay;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class HypotheticalEventTest {
    @Test void typedControlsProduceProtocolActions() {
        var event = new HypotheticalEvent.Morph(3, Optional.of(2), Optional.empty());
        var action = ReplayActionFactory.hypotheticalControl(new HypotheticalControl.Event(event));
        assertEquals("hypothetical", action.path("type").asText());
        assertEquals("event", action.path("control").path("type").asText());
        assertEquals("morph", action.path("control").path("event").path("type").asText());
    }
    @Test void validatesHypotheticalBounds() {
        assertThrows(IllegalArgumentException.class, () -> new HypotheticalEvent.Morph(3, Optional.of(6), Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new HypotheticalEvent.Card("play", 6, 1, Optional.empty()));
    }
}
