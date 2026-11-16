package com.pimak.hanab.client.transport;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class HttpJsonTransportTest {
    HttpServer server; HttpJsonTransport transport;
    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/login", e -> { assertEquals("POST", e.getRequestMethod()); assertEquals("application/x-www-form-urlencoded", e.getRequestHeaders().getFirst("Content-Type")); e.getResponseHeaders().add("Set-Cookie", "session=abc; Path=/"); respond(e, 200, "{\"userId\":1}"); });
        server.createContext("/api/v1/identity/token", e -> { assertEquals("session=abc", e.getRequestHeaders().getFirst("Cookie")); respond(e, 200, "{\"username\":\"max\",\"token\":\"identity\"}"); });
        server.createContext("/api/v1/variants", e -> respond(e, 200, "{\"No Variant\":\"No Variant\"}"));
        server.createContext("/api/v1/variants-full", e -> respond(e, 200, "{\"No Variant\":{\"name\":\"No Variant\",\"suits\":[\"Red\"],\"stackSize\":3,\"maxScore\":3}}"));
        server.createContext("/api/v1/history/Alice", e -> respond(e, 200, "{\"games\":[{\"gameId\":1,\"options\":{},\"seed\":\"seed\",\"score\":25,\"numTurns\":10,\"endCondition\":\"normal\",\"startedAt\":\"2026-01-01T00:00:00Z\",\"finishedAt\":\"2026-01-01T00:10:00Z\",\"numGamesOnSeed\":1,\"playerNames\":[\"Alice\"],\"tags\":[]}],\"offset\":0,\"total\":1}"));
        server.createContext("/api/v1/seed/seed", e -> respond(e, 200, "{\"games\":[],\"offset\":0,\"total\":0}"));
        server.createContext("/api/v1/tag/", e -> respond(e, 200, "[{\"gameId\":1,\"options\":{},\"seed\":\"seed\",\"score\":25,\"numTurns\":10,\"endCondition\":\"normal\",\"startedAt\":\"2026-01-01T00:00:00Z\",\"finishedAt\":\"2026-01-01T00:10:00Z\",\"numGamesOnSeed\":1,\"playerNames\":[\"Alice\",\"Bob\"],\"tags\":[\"special tag\"]}]"));
        server.createContext("/export/123", e -> respond(e, 200, "{\"seed\":\"abc\",\"playerNames\":[\"Alice\"],\"actions\":[{\"type\":\"play\",\"order\":0}]}"));
        server.createContext("/password-reset", e -> { assertEquals("POST", e.getRequestMethod()); respond(e, 200, "{\"hash\":\"argon2-hash\"}"); });
        server.start(); transport = new HttpJsonTransport(URI.create("http://localhost:" + server.getAddress().getPort()));
    }
    @AfterEach void stop() { transport.close(); server.stop(0); }
    @Test void loginStoresCookieAndIdentityUsesIt() {
        assertEquals(1L, ((Number) transport.login("max", "secret").join().get("userId")).longValue());
        assertEquals("max", transport.identity().join().get("username"));
        assertTrue(transport.hasSession());
    }
    @Test void catalogAndReplayDecodeJson() {
        assertEquals("No Variant", transport.catalog().join().get("No Variant"));
        assertEquals("abc", transport.replay("123").join().get("seed"));
        assertThrows(CompletionException.class, () -> transport.replay("a-b").join());
        assertEquals(3, transport.variantCatalog().join().variant("No Variant").orElseThrow().maxScore());
    }
    @Test void passwordHashUsesJsonAndReturnsOnlyTheHash() {
        assertEquals("argon2-hash", transport.passwordHash("secret").join());
        assertThrows(CompletionException.class, () -> transport.passwordHash(" ").join());
    }
    @Test void replayDocumentCanBeDecodedIntoThePublicModel() {
        assertEquals("abc", transport.replayDocumentDecoded("123").join().seed().orElseThrow());
        assertThrows(CompletionException.class, () -> transport.replayDocumentDecoded("no").join());
    }
    @Test void publicHistoryAndSeedEndpointsHaveTypedPages() {
        var page = transport.history(java.util.List.of("Alice"), new com.pimak.hanab.client.catalog.HistoryQuery(0, 10)).join();
        assertEquals(1, page.total());
        assertEquals("seed", page.games().get(0).seed());
        assertEquals(0, transport.seedGames(new com.pimak.hanab.client.catalog.SeedGamesQuery("seed", 0, 10)).join().page().total());
    }
    @Test void taggedGamesAcceptsArrayResponsesAndEncodesTagPath() {
        var games = transport.taggedGames("special tag").join();
        assertEquals(1, games.size());
        assertEquals(1L, ((Number) games.get(0).get("gameId")).longValue());
        assertEquals(25, transport.taggedGameEntries("special tag").join().get(0).score());
    }
    @Test void nonSuccessIsTypedWithoutEchoingSecret() {
        server.createContext("/fail", e -> respond(e, 401, "password=secret"));
        TransportException ex = assertThrows(CompletionException.class, () -> transport.get("/fail").join()).getCause() instanceof TransportException t ? t : null;
        assertNotNull(ex); assertEquals(401, ex.statusCode()); assertFalse(ex.getMessage().contains("secret"));
    }
    @Test void nonSuccessPreservesStructuredNoticeMetadata() {
        server.createContext("/notice", e -> respond(e, 429, "{\"message\":\"slow down\",\"code\":\"rateLimited\",\"diagnosticId\":\"diag-1\"}"));
        TransportException ex = assertThrows(CompletionException.class, () -> transport.get("/notice").join()).getCause() instanceof TransportException t ? t : null;
        assertNotNull(ex); assertEquals(429, ex.statusCode()); assertEquals("rateLimited", ex.code()); assertEquals("diag-1", ex.diagnosticId()); assertEquals("slow down", ex.getMessage());
    }
    static void respond(com.sun.net.httpserver.HttpExchange e, int status, String body) throws java.io.IOException { byte[] b=body.getBytes(StandardCharsets.UTF_8); e.getResponseHeaders().add("Content-Type","application/json"); e.sendResponseHeaders(status,b.length); try (var output = e.getResponseBody()) { output.write(b); } }
}
