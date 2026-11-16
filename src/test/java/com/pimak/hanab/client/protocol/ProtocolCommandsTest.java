package com.pimak.hanab.client.protocol;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolCommandsTest {
    @Test void validatesPhaseTwoCommands() throws Exception {
        assertEquals("tableLeave", ProtocolCodec.decode("tableLeave {\"tableId\":1}").command());
        assertEquals("tableCreate", ProtocolCodec.decode("tableCreate {\"settings\":{},\"password\":\"secret\"}").command());
        assertEquals("tableUpdate", ProtocolCodec.decode("tableUpdate {\"tableId\":1,\"settings\":{}}").command());
        assertEquals("tableSetOwner", ProtocolCodec.decode("tableSetOwner {\"tableId\":1,\"userId\":2}").command());
        assertEquals("friendAdd", ProtocolCodec.decode("friendAdd {\"username\":\"Bob\"}").command());
        assertEquals("pause", ProtocolCodec.decode("pause {\"tableId\":1,\"setting\":\"pause\"}").command());
        assertEquals("playerAction", ProtocolCodec.decode("playerAction {\"tableId\":1,\"action\":{\"type\":\"play\",\"order\":2}}").command());
        assertEquals("chatTyping", ProtocolCodec.decode("chatTyping {\"tableId\":1}").command());
        assertDoesNotThrow(() -> ProtocolCodec.decode("tableStart {\"tableId\":1,\"intendedPlayerNames\":[\"Alice\"]}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("tableRestartDecline {\"tableId\":1,\"declined\":true}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("settings {\"settings\":{},\"cleared\":[]}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("userInactive {\"inactive\":true}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("idleReset {\"tableId\":1}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("chatRead {\"tableId\":1}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("linkList {}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("replayAction {\"tableId\":1,\"action\":{\"type\":\"turn\",\"turn\":4}}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("replayAction {\"tableId\":1,\"action\":{\"type\":\"efficiency\",\"modifier\":-60}}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("replayAction {\"tableId\":1,\"action\":{\"type\":\"arrow\",\"target\":{\"type\":\"board\",\"element\":\"maxScore\"}}}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("replayCreate {\"source\":{\"type\":\"saved\",\"gameId\":3},\"visibility\":{\"type\":\"solo\"}}"));
        assertDoesNotThrow(() -> ProtocolCodec.decode("replayCreate {\"source\":{\"type\":\"imported\",\"game\":{}},\"visibility\":{\"type\":\"shared\",\"joinExisting\":true,\"spoilerFree\":false},\"shadowingPlayerIndex\":0}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("playerAction {\"tableId\":1,\"action\":{\"type\":\"play\",\"order\":66}}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("replayAction {\"tableId\":1,\"action\":{\"type\":\"unknown\"}}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("replayAction {\"tableId\":1,\"action\":{\"type\":\"turn\",\"turn\":513}}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("replayAction {\"tableId\":1,\"action\":{\"type\":\"sound\",\"sound\":\"laser\"}}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("replayCreate {\"source\":{\"type\":\"saved\",\"gameId\":0},\"visibility\":{\"type\":\"solo\"}}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("replayCreate {\"source\":{\"type\":\"saved\",\"gameId\":3},\"visibility\":{\"type\":\"shared\",\"joinExisting\":true}}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("tableSpectate {\"tableId\":1,\"shadowingPlayerIndex\":6}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("tableJoin {\"tableId\":1,\"password\":\"\"}"));
        assertThrows(ProtocolDecodeException.class, () -> ProtocolCodec.decode("note {\"tableId\":1,\"order\":2}"));
    }
}
