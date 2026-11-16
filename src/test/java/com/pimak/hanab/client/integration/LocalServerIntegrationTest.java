package com.pimak.hanab.client.integration;

import com.pimak.hanab.client.api.HanabBot;
import com.pimak.hanab.client.api.HanabClient;
import com.pimak.hanab.client.api.LoginOptions;
import com.pimak.hanab.client.protocol.WireMessage;
import com.pimak.hanab.client.api.ServerEvent;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Flow;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalServerIntegrationTest {
    @Test
    void botUsersCanAuthenticateOpenWebSocketsAndLoadLobby() throws Exception {
        var config = TestConfig.load();
        Assumptions.assumeTrue(config != null, "No .env.local or HANAB_INTEGRATION_CONFIG configured");
        try (HanabClient first = HanabBot.connect(config.server()); HanabClient second = HanabBot.connect(config.server())) {
            var firstSession = login(first, config.credentialsA());
            var secondSession = login(second, config.credentialsB());
            assertEquals(config.userA(), firstSession.username().value());
            assertEquals(config.userB(), secondSession.username().value());
            assertEquals(config.userA(), first.identity().toCompletableFuture().get(20, TimeUnit.SECONDS).username().value());
            assertEquals(config.userB(), second.identity().toCompletableFuture().get(20, TimeUnit.SECONDS).username().value());
            assertNotNull(loadLobby(first));
            assertNotNull(loadLobby(second));
        }
    }

    @Test
    void botUserCanReconnectAndReloadTheLobby() throws Exception {
        var config = TestConfig.load();
        Assumptions.assumeTrue(config != null, "No .env.local or HANAB_INTEGRATION_CONFIG configured");
        try (HanabClient client = HanabBot.connect(config.server())) {
            login(client, config.credentialsA());
            assertNotNull(loadLobby(client));
            client.reconnect().toCompletableFuture().get(20, TimeUnit.SECONDS);
            assertNotNull(loadLobby(client));
        }
    }

    @Test
    void botUsersCanCreateJoinStartAndLoadAGame() throws Exception {
        var config = TestConfig.load();
        Assumptions.assumeTrue(config != null, "No .env.local or HANAB_INTEGRATION_CONFIG configured");
        try (HanabClient owner = HanabBot.connect(config.server()); HanabClient player = HanabBot.connect(config.server())) {
            login(owner, config.credentialsA());
            login(player, config.credentialsB());
            var ownerEvents = new EventQueue(owner);
            var playerEvents = new EventQueue(player);
            owner.createTable(new com.pimak.hanab.client.model.TableSettings("bot integration", 2,
                            new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode()), null)
                    .toCompletableFuture().get(20, TimeUnit.SECONDS);
            var joined = ownerEvents.next(message -> message.command().equals("tableJoined"), 20);
            long tableId = joined.payload().path("tableId").asLong(-1);
            assertEquals(true, tableId > 0);

            player.joinTable(new com.pimak.hanab.client.model.Ids.TableId(tableId), null)
                    .toCompletableFuture().get(20, TimeUnit.SECONDS);
            assertEquals(tableId, playerEvents.next(message -> message.command().equals("tableJoined"), 20)
                    .payload().path("tableId").asLong(-1));

            owner.startTable(new com.pimak.hanab.client.model.Ids.TableId(tableId),
                    java.util.List.of(owner.identitySnapshot().toCompletableFuture().get(20, TimeUnit.SECONDS).userId().orElseThrow(),
                            player.identitySnapshot().toCompletableFuture().get(20, TimeUnit.SECONDS).userId().orElseThrow()))
                    .toCompletableFuture().get(20, TimeUnit.SECONDS);
            assertEquals(tableId, ownerEvents.next(message -> message.command().equals("tableStarted"), 20)
                    .payload().path("tableId").asLong(-1));

            assertEquals(tableId, ownerEvents.next(message -> message.command().equals("gameInit"), 20)
                    .payload().path("tableId").asLong(-1));

            var state = ownerEvents.next(message -> message.command().equals("gameState"), 20);
            assertEquals(tableId, state.payload().path("tableId").asLong(-1));
            assertTrue(state.payload().path("events").isArray());
            assertEquals(tableId, owner.gameState(new com.pimak.hanab.client.model.Ids.TableId(tableId)).orElseThrow()
                    .tableId().orElseThrow());
            var ownerState = owner.gameState(new com.pimak.hanab.client.model.Ids.TableId(tableId)).orElseThrow();
            assertTrue(ownerState.playerNames().contains(config.userA()));
            // Prefer the authoritative snapshot field; older fixtures may only expose a turn event.
            int currentPlayer = state.payload().path("currentPlayerIndex").isNumber()
                    ? state.payload().path("currentPlayerIndex").intValue()
                    : ownerState.currentPlayerIndex().orElse(0);
            HanabClient actor = ownerState.playerNames().get(currentPlayer).equals(config.userA()) ? owner : player;
            EventQueue actorEvents = actor == owner ? ownerEvents : playerEvents;
            var actorState = actor.gameState(new com.pimak.hanab.client.model.Ids.TableId(tableId)).orElseThrow();
            int order = actorState.hands().get(currentPlayer).stream().findFirst().orElseThrow();
            actor.play(new com.pimak.hanab.client.model.Ids.TableId(tableId), new com.pimak.hanab.client.model.Ids.CardOrder(order))
                    .toCompletableFuture().get(20, TimeUnit.SECONDS);
            var actionEvent = actorEvents
                    .next(message -> (message.command().equals("gameEvent") && message.payload().path("tableId").asLong(-1) == tableId)
                            || message.command().equals("error") || message.command().equals("warning"), 20);
            assertEquals("gameEvent", actionEvent.command(), "unexpected server response: " + actionEvent.payload());
            assertEquals(tableId, actionEvent.payload().path("tableId").asLong(-1));
            owner.terminate(new com.pimak.hanab.client.model.Ids.TableId(tableId)).toCompletableFuture()
                    .get(20, TimeUnit.SECONDS);
            var finished = ownerEvents.next(message -> message.command().equals("gameFinished")
                    || message.command().equals("error"), 20);
            assertEquals("gameFinished", finished.command(), "unexpected termination response: " + finished.payload());
        }
    }

    private static final class EventQueue implements Flow.Subscriber<ServerEvent> {
        private final LinkedBlockingQueue<ServerEvent> events = new LinkedBlockingQueue<>();
        EventQueue(HanabClient client) { client.events().subscribe(this); }
        @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
        @Override public void onNext(ServerEvent item) { events.offer(item); }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
        WireMessage next(Predicate<WireMessage> predicate, int seconds) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
            var seen = new java.util.ArrayList<String>();
            while (System.nanoTime() < deadline) {
                ServerEvent event = events.poll(1, TimeUnit.SECONDS);
                if (event != null) { seen.add(event.message().command() + " " + event.message().payload()); if (predicate.test(event.message())) return event.message(); }
            }
            throw new AssertionError("Timed out waiting for server event; seen=" + seen);
        }
    }

    private static com.pimak.hanab.client.model.Session login(HanabClient client,
                                                              com.pimak.hanab.client.api.Credentials credentials) throws Exception {
        boolean createAccount = true;
        for (int attempt = 0; ; attempt++) {
            try {
                return client.login(credentials, LoginOptions.bot(createAccount)).toCompletableFuture().get(20, TimeUnit.SECONDS);
            } catch (java.util.concurrent.ExecutionException error) {
                String message = error.getCause() == null ? "" : String.valueOf(error.getCause().getMessage());
                if (createAccount && message.contains("HTTP 429")) {
                    // A previous test may already have provisioned the account. Reuse it
                    // through the normal bot login path instead of retrying account creation.
                    createAccount = false;
                    continue;
                }
                if (attempt >= 30 || !message.contains("HTTP 429")) throw error;
                Thread.sleep(3_000L);
            }
        }
    }

    private static com.pimak.hanab.client.model.LobbySnapshot loadLobby(HanabClient client) throws Exception {
        for (int attempt = 0; ; attempt++) {
            try { return client.loadLobby().toCompletableFuture().get(20, TimeUnit.SECONDS); }
            catch (java.util.concurrent.ExecutionException error) {
                String message = error.getCause() == null ? "" : String.valueOf(error.getCause().getMessage());
                if (attempt >= 30 || !message.contains("HTTP 429")) throw error;
                Thread.sleep(3_000L);
            }
        }
    }

    static record TestConfig(URI server, String userA, String passwordA, String userB, String passwordB) {
        static TestConfig load() throws IOException {
            if (!Boolean.getBoolean("hanab.integration.run")) return null;
            Path path = Path.of(System.getProperty("hanab.integration.config", ".env.local"));
            String configured = System.getenv("HANAB_INTEGRATION_CONFIG");
            if (configured != null && !configured.isBlank()) path = Path.of(configured);
            Map<String, String> values = new HashMap<>();
            if (Files.isRegularFile(path)) {
                for (String line : Files.readAllLines(path)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int equals = trimmed.indexOf('=');
                    if (equals > 0) values.put(trimmed.substring(0, equals).trim(), trimmed.substring(equals + 1).trim());
                }
            }
            for (String key : java.util.List.of("HANAB_TEST_SERVER", "HANAB_TEST_USER_A", "HANAB_TEST_PASSWORD_A", "HANAB_TEST_USER_B", "HANAB_TEST_PASSWORD_B")) {
                String value = System.getenv(key);
                if (value != null && !value.isBlank()) values.putIfAbsent(key, value.trim());
            }
            if (values.isEmpty()) return null;
            return new TestConfig(URI.create(required(values, "HANAB_TEST_SERVER")), required(values, "HANAB_TEST_USER_A"),
                    required(values, "HANAB_TEST_PASSWORD_A"), required(values, "HANAB_TEST_USER_B"), required(values, "HANAB_TEST_PASSWORD_B"));
        }
        private static String required(Map<String, String> values, String key) { String value = values.get(key); if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key); return value; }
        com.pimak.hanab.client.api.Credentials credentialsA() { return new com.pimak.hanab.client.api.Credentials(userA, passwordA.toCharArray()); }
        com.pimak.hanab.client.api.Credentials credentialsB() { return new com.pimak.hanab.client.api.Credentials(userB, passwordB.toCharArray()); }
    }
}
