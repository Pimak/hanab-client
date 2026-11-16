package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TypedMetadataTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void projectsNotesSpectatorsClockAndStatsWithoutLosingRawFields() throws Exception {
        var state = mapper.readTree("""
                {"connected":[true,false],"notes":[{"order":3,"text":"keep"}],
                 "spectators":[{"username":"Watcher","userId":9}],
                 "clocks":{"playerTimesMs":[1000,2000],"currentPlayerIndex":1,"turnElapsedMs":25},
                 "stats":{"score":12,"maxScore":25,"strikes":1},"mode":"spectating","future":true}
                """);
        var metadata = GameStateMetadata.from(state);
        assertEquals("keep", metadata.cardNotes().get(0).text());
        assertEquals("Watcher", metadata.spectatorList().get(0).username());
        assertEquals(2, metadata.clock().orElseThrow().playerTimesMs().size());
        assertEquals(12, metadata.stats().orElseThrow().score().orElseThrow());
        assertEquals(Perspective.SPECTATOR, metadata.perspective());
        assertTrue(metadata.unknownFields().has("future"));
    }

    @Test void flattensCanonicalPlayerNotesByCardOrder() throws Exception {
        var state = mapper.readTree("{\"connected\":[true,false],\"notes\":[{\"username\":\"A\",\"notes\":[\"red\",\"\"],\"spectator\":false}],\"spectators\":[]}");
        var metadata = GameStateMetadata.from(state);
        assertEquals(1, metadata.cardNotes().size());
        assertEquals(0, metadata.cardNotes().get(0).order());
        assertEquals("red", metadata.cardNotes().get(0).text());
    }

    @Test void rejectsMalformedClockShape() throws Exception {
        var state = mapper.readTree("{\"connected\":[true],\"notes\":[],\"spectators\":[],\"clocks\":{\"playerTimesMs\":[],\"turnElapsedMs\":0}}");
        assertThrows(IllegalArgumentException.class, () -> GameStateMetadata.from(state).clock().orElseThrow());
    }
}
