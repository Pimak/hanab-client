package com.pimak.hanab.client.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.pimak.hanab.client.model.User;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProtocolCodecTest {
    @Test
    void encodesCamelCaseCommandEnvelope() throws Exception {
        var command = new ChatCommand("hello", 12L);
        var json = ProtocolCodec.encode(command);
        assertEquals("chat {\"text\":\"hello\",\"tableId\":12}", json);
    }

    @Test
    void decodesKnownUserAndPreservesOnlyAllowedUnknownFields() throws Exception {
        var user = ProtocolCodec.decodeUser("{\"userId\":7,\"username\":\"Alice\",\"future\":true}");
        assertEquals(new User(7, "Alice", Map.of("future", true)), user);
        assertEquals(true, user.unknownFields().get("future"));
    }

    @Test
    void rejectsMalformedJsonAndUnknownCommandPayloadFields() {
        var malformed = assertThrows(ProtocolDecodeException.class,
                () -> ProtocolCodec.decodeUser("{\"userId\":7"));
        assertEquals(ProtocolDecodeException.Code.INVALID_JSON, malformed.code());

        var unknown = assertThrows(ProtocolDecodeException.class,
                () -> ProtocolCodec.decode("chat {\"text\":\"x\",\"unexpected\":1}"));
        assertEquals(ProtocolDecodeException.Code.UNKNOWN_FIELD, unknown.code());
    }

    @Test
    void rejectsTableIdsAboveJsonSafeInteger() {
        assertDoesNotThrow(() -> ProtocolCodec.decode(
                "tableJoin {\"tableId\":9007199254740991}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode(
                "tableJoin {\"tableId\":9007199254740992}"));
        assertThrows(IllegalArgumentException.class,
                () -> new TableJoinCommand(9_007_199_254_740_992L));
    }
}
