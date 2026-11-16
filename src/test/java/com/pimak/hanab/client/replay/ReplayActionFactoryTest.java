package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayActionFactoryTest {
    @Test void buildsHypotheticalControls() {
        assertEquals("start", ReplayActionFactory.hypotheticalStart().path("control").path("type").asText());
        assertTrue(ReplayActionFactory.showDrawnCards(true).path("control").path("show").asBoolean());
        var event = JsonNodeFactory.instance.objectNode().put("type", "back");
        assertThrows(IllegalArgumentException.class, () -> ReplayActionFactory.hypotheticalEvent(event));
        var invalidDraw = JsonNodeFactory.instance.objectNode().put("type", "draw");
        assertThrows(IllegalArgumentException.class, () -> ReplayActionFactory.hypotheticalEvent(invalidDraw));
        var validDraw = JsonNodeFactory.instance.objectNode().put("type", "draw").put("playerIndex", 0).put("order", 2);
        assertDoesNotThrow(() -> ReplayActionFactory.hypotheticalEvent(validDraw));
    }
}
