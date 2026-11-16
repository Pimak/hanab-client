package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.protocol.WireMessage;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventSocialTest {
    @Test void decodesPublicChatAndPrivateHistory() throws Exception {
        var mapper = new ObjectMapper();
        var chat = mapper.readTree("{\"text\":\"hello\",\"sentAt\":\"2026-01-01T00:00:00Z\",\"chatId\":9,\"kind\":{\"type\":\"public\",\"author\":{\"type\":\"user\",\"username\":\"Alice\"}}}");
        var event = new ServerEvent(Instant.now(), new WireMessage("chat", chat));
        assertEquals("Alice", event.chat().orElseThrow().author().orElseThrow());
        var history = new ServerEvent(Instant.now(), new WireMessage("chatPmHistory", mapper.readTree("{\"messages\":[" + chat + "]}")));
        assertEquals(1, history.chatList().orElseThrow().size());
    }

    @Test void acceptsTableZeroAndLargeUnreadCount() throws Exception {
        var mapper = new ObjectMapper();
        var chat = mapper.readTree("{\"text\":\"hello\",\"sentAt\":\"2026-01-01T00:00:00Z\",\"chatId\":9,\"tableId\":0,\"kind\":{\"type\":\"public\",\"author\":{\"type\":\"user\",\"username\":\"Alice\"}}}");
        var chatEvent = new ServerEvent(Instant.now(), new WireMessage("chat", chat));
        var event = new ServerEvent(Instant.now(), new WireMessage("chatList", mapper.readTree("{\"messages\":[" + chat + "],\"numUnread\":9007199254740991}")));
        assertEquals(0, chatEvent.chat().orElseThrow().tableId().value().orElseThrow());
        assertEquals(9_007_199_254_740_991L, event.chatUnreadCount().orElseThrow());
        assertEquals(1, event.chatList().orElseThrow().size());
    }
}
