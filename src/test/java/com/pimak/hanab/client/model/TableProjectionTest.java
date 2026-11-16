package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TableProjectionTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void exposesTypedRosterSettingsOwnerAndSpectators() throws Exception {
        var table = new Table(new Ids.TableId(3), "pregame", "Alice", ProtocolFixture.table());
        assertEquals(java.util.List.of("Alice"), table.playerNames());
        assertEquals("Alice", table.ownerUser().orElseThrow().username());
        assertEquals(5, table.settings().orElseThrow().maxPlayers());
        assertEquals("Carol", table.spectators().get(0).username());
        assertEquals(0, table.progress().orElseThrow());
    }

    @Test void rejectsMalformedTypedTableFields() throws Exception {
        var table = new Table(new Ids.TableId(3), "pregame", "Alice", ProtocolFixture.invalidPlayers());
        assertThrows(IllegalStateException.class, table::playerNames);
    }

    private static final class ProtocolFixture {
        static com.fasterxml.jackson.databind.JsonNode table() throws Exception {
            return new ObjectMapper().readTree("""
                {"tableId":3,"owner":{"userId":7,"username":"Alice"},"passwordProtected":false,
                 "playerNames":["Alice"],"progress":0,"phase":"pregame",
                 "spectators":[{"userId":9,"username":"Carol"}],
                 "settings":{"name":"game","maxPlayers":5,"options":{"variantId":0}}}
                """);
        }
        static com.fasterxml.jackson.databind.JsonNode invalidPlayers() throws Exception {
            return new ObjectMapper().readTree("{\"tableId\":3,\"playerNames\":[{}]}");
        }
    }
}
