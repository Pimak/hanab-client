package com.pimak.hanab.client.transport;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.junit.jupiter.api.Assertions.*;

class WebSocketTransportTest {
    @Test void refusedConnectionCompletesAndDoesNotLogHandshakeSecrets() {
        List<WebSocketEvent> events = new CopyOnWriteArrayList<>();
        WebSocketTransport transport = new WebSocketTransport(URI.create("ws://localhost:1"), java.util.Map.of("token", "secret"), events::add, Duration.ofMillis(200), 0, Duration.ZERO);
        try {
            var failure = assertThrows(java.util.concurrent.CompletionException.class, () -> transport.connect().join());
            assertInstanceOf(TransportException.class, failure.getCause());
            assertTrue(events.stream().anyMatch(e -> e.type() == WebSocketEvent.Type.ERROR));
            assertTrue(events.stream().noneMatch(e -> "secret".equals(e.text())));
        } finally {
            transport.close();
        }
    }
}
