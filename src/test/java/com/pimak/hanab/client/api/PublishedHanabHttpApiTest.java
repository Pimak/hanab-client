package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.Ids;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PublishedHanabHttpApiTest {
    private HttpServer server;
    private URI target;

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertNull(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] body = "<html>Hanab</html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/unavailable", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.createContext("/export/42", exchange -> {
            byte[] body = "{\"seed\":\"abc\",\"playerNames\":[\"Alice\"],\"actions\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/export/43", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        target = URI.create("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach void stop() {
        server.stop(0);
    }

    @Test void probeAcceptsHtmlAndReturnsUsableSuccessStatus() {
        var result = new PublishedHanabAvailabilityProbe(Duration.ofSeconds(1)).probe(target).toCompletableFuture().join();
        assertEquals(HttpProbeStatus.REACHABLE, result.status());
        assertEquals(200, result.httpStatus().orElseThrow());
        assertTrue(result.reachable());
    }

    @Test void probeClassifiesHttpErrorsAndBlockedTargets() {
        var probe = new PublishedHanabAvailabilityProbe(Duration.ofSeconds(1));
        HttpServer unavailable = serverWithRootResponse(503, 0);
        unavailable.start();
        var error = probe.probe(URI.create("http://localhost:" + unavailable.getAddress().getPort())).toCompletableFuture().join();
        unavailable.stop(0);
        assertEquals(HttpProbeStatus.HTTP_ERROR, error.status());
        assertEquals(503, error.httpStatus().orElseThrow());
        var blocked = probe.probe(URI.create("http://example.com")).toCompletableFuture().join();
        assertEquals(HttpProbeStatus.INVALID_OR_BLOCKED_TARGET, blocked.status());
        assertTrue(blocked.httpStatus().isEmpty());
    }

    @Test void probeClassifiesTimeout() {
        HttpServer slow = serverWithRootResponse(200, 500);
        slow.start();
        var result = new PublishedHanabAvailabilityProbe(Duration.ofMillis(25))
                .probe(URI.create("http://localhost:" + slow.getAddress().getPort())).toCompletableFuture().join();
        slow.stop(0);
        assertEquals(HttpProbeStatus.TIMEOUT, result.status());
    }

    @Test void probeClassifiesConnectionRefused() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        var result = new PublishedHanabAvailabilityProbe(Duration.ofMillis(250))
                .probe(URI.create("http://localhost:" + port)).toCompletableFuture().join();
        assertEquals(HttpProbeStatus.CONNECTION_REFUSED, result.status());
    }

    @Test void fetcherDecodesReplayAndExposesHttpStatusOnFailure() {
        var fetcher = new PublishedHanabReplayFetcher(target, Duration.ofSeconds(1));
        assertEquals("abc", fetcher.fetch(new Ids.GameId(42)).toCompletableFuture().join().seed().orElseThrow());
        TransportException error = assertThrows(CompletionException.class,
                () -> fetcher.fetch(new Ids.GameId(43)).toCompletableFuture().join()).getCause() instanceof TransportException exception ? exception : null;
        assertNotNull(error);
        assertEquals(404, error.statusCode());
    }

    private static HttpServer serverWithRootResponse(int status, long delayMillis) {
        try {
            HttpServer result = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            result.createContext("/", exchange -> {
                try {
                    if (delayMillis > 0) Thread.sleep(delayMillis);
                    exchange.sendResponseHeaders(status, -1);
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            return result;
        } catch (java.io.IOException error) {
            throw new AssertionError(error);
        }
    }
}
