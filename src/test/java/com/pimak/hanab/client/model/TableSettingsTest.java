package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableSettingsTest {
    @Test
    void serializesAndRestoresExtensibleSettings() {
        var options = new ObjectMapper().createObjectNode().put("speedrun", true);
        var deal = new ObjectMapper().createObjectNode().put("type", "seed").put("suffix", "p2v0s1");
        var settings = new TableSettings("practice", 2, options, Optional.of(deal));
        var restored = TableSettings.fromJson(settings.toJson());
        assertEquals(settings.name(), restored.name());
        assertEquals(settings.maxPlayers(), restored.maxPlayers());
        assertEquals(settings.options(), restored.options());
        assertEquals(settings.deal(), restored.deal());
    }

    @Test
    void rejectsInvalidPlayerCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> new TableSettings("invalid", 7, new ObjectMapper().createObjectNode()));
    }

    @Test
    void rejectsInvalidOptionsAndDeals() {
        assertThrows(IllegalArgumentException.class,
                () -> new TableSettings("invalid", 2, new ObjectMapper().createArrayNode()));
        assertThrows(IllegalArgumentException.class,
                () -> new TableSettings("invalid", 2, new ObjectMapper().createObjectNode(),
                        Optional.of(new ObjectMapper().createObjectNode().put("type", "replay").put("gameId", 0))));
    }

    @Test
    void exposesTypedGameOptions() {
        var options = new GameOptions(3, Optional.of(new TimeControl(60, 5)), true, false, false, false, false, false, false);
        var settings = new TableSettings("typed", 2, options);
        assertEquals(3, settings.gameOptions().variantId());
        assertEquals(60, settings.gameOptions().timeControl().orElseThrow().baseSeconds());
    }
}
