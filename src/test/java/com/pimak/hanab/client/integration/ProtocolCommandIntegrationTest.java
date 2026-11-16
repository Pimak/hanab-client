package com.pimak.hanab.client.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pimak.hanab.client.api.HanabBot;
import com.pimak.hanab.client.api.HanabClientImpl;
import com.pimak.hanab.client.api.LoginOptions;
import com.pimak.hanab.client.api.ServerEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/** Sends every client command declared by the upstream protocol to a real server. */
class ProtocolCommandIntegrationTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> COMMANDS = List.of(
            "lobbyLoad", "sync", "chat", "chatPm", "chatPmHistory", "friendAdd", "friendRemove",
            "linkAdd", "linkRemove", "linkList", "blockAdd", "blockRemove", "chatRead", "chatTyping",
            "tableJoin", "tableLeave", "tableReattend", "tableSetOwner", "tableStart", "tableRestart",
            "tableRestartDecline", "tableSuggest", "tableSpectate", "tableUnattend", "tableTerminate",
            "tableTerminateVote", "gameInitLoad", "gameStateLoad", "gameLoaded", "settings", "userInactive",
            "gameHistory", "seedGames", "tagSearch", "tableCreate", "tableUpdate", "replayCreate",
            "playerAction", "pause", "idleReset", "note", "tagAdd", "tagRemove", "tagRemoveAll", "replayAction");

    @Test
    void everyUpstreamClientCommandCanBeSentToTheServer() throws Exception {
        var config = LocalServerIntegrationTest.TestConfig.load();
        Assumptions.assumeTrue(config != null, "Integration server configuration is not available");

        var failures = new ArrayList<String>();
        try (HanabClientImpl client = (HanabClientImpl) HanabBot.connect(config.server())) {
            client.login(config.credentialsA(), LoginOptions.bot(true)).toCompletableFuture().get(30, TimeUnit.SECONDS);
            var events = new EventQueue(client);
            for (String command : COMMANDS) {
                try {
                    client.command(command, payload(command)).toCompletableFuture().get(10, TimeUnit.SECONDS);
                    // Give the server a chance to parse and route the message before the next one.
                    events.drainFor(150);
                } catch (Throwable error) {
                    failures.add(command + ": " + describe(error));
                }
            }
        }
        if (!failures.isEmpty()) {
            fail("Commands that failed: " + String.join(", ", failures));
        }
    }

    private static ObjectNode payload(String command) {
        var payload = JSON.createObjectNode();
        switch (command) {
            case "chat" -> payload.put("text", "compatibility test");
            case "chatPm" -> payload.put("text", "compatibility test").put("recipient", "ci-command-peer");
            case "chatPmHistory" -> payload.put("limit", 1);
            case "friendAdd", "friendRemove", "linkAdd", "linkRemove", "blockAdd", "blockRemove" ->
                    payload.put("username", "ci-command-peer");
            case "chatRead", "chatTyping", "tableJoin", "tableLeave", "tableReattend", "tableUnattend",
                    "tableTerminate", "tableTerminateVote", "gameInitLoad", "gameStateLoad", "gameLoaded",
                    "idleReset", "tagRemoveAll" -> payload.put("tableId", 1);
            case "tableSetOwner" -> payload.put("tableId", 1).put("userId", 1);
            case "tableStart" -> payload.put("tableId", 1).set("intendedPlayerNames", JSON.createArrayNode());
            case "tableRestart" -> payload.put("tableId", 1).put("hidePregame", false);
            case "tableRestartDecline" -> payload.put("tableId", 1).put("declined", false);
            case "tableSuggest" -> payload.put("tableId", 1).put("turn", 0);
            case "tableSpectate" -> payload.put("tableId", 1);
            case "settings" -> {
                payload.set("settings", JSON.createObjectNode());
                payload.set("cleared", JSON.createArrayNode());
            }
            case "userInactive" -> payload.put("inactive", false);
            case "gameHistory" -> payload.put("offset", 0).put("limit", 1);
            case "seedGames" -> payload.put("seed", "p2v0s1").put("offset", 0).put("limit", 1);
            case "tagSearch" -> payload.put("tag", "compatibility");
            case "tableCreate" -> payload.set("settings", JSON.createObjectNode()
                    .put("name", "compatibility")
                    .put("variantName", "No Variant")
                    .put("maxPlayers", 2)
                    .put("timed", false));
            case "tableUpdate" -> payload.put("tableId", 1).set("settings", JSON.createObjectNode());
            case "replayCreate" -> {
                var source = JSON.createObjectNode().put("type", "imported");
                source.set("game", JSON.createObjectNode());
                payload.set("source", source);
                payload.set("visibility", JSON.createObjectNode().put("type", "solo"));
            }
            case "playerAction" -> {
                payload.put("tableId", 1);
                payload.set("action", JSON.createObjectNode().put("type", "play").put("order", 0));
            }
            case "pause" -> payload.put("tableId", 1).put("setting", "pause");
            case "note" -> payload.put("tableId", 1).put("order", 0).put("text", "compatibility");
            case "tagAdd", "tagRemove" -> payload.put("tableId", 1).put("tag", "compatibility");
            case "replayAction" -> {
                payload.put("tableId", 1);
                payload.set("action", JSON.createObjectNode().put("type", "turn").put("turn", 0));
            }
            default -> { }
        }
        return payload;
    }

    private static String describe(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null && current.getClass().getName().contains("CompletionException")) {
            current = current.getCause();
        }
        return current.getClass().getSimpleName() + ": " + current.getMessage();
    }

    private static final class EventQueue implements Flow.Subscriber<ServerEvent> {
        private final LinkedBlockingQueue<ServerEvent> events = new LinkedBlockingQueue<>();
        EventQueue(HanabClientImpl client) { client.events().subscribe(this); }
        @Override public void onSubscribe(Flow.Subscription subscription) { subscription.request(Long.MAX_VALUE); }
        @Override public void onNext(ServerEvent event) { events.offer(event); }
        @Override public void onError(Throwable throwable) { }
        @Override public void onComplete() { }
        void drainFor(long millis) throws InterruptedException {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis);
            while (System.nanoTime() < deadline) events.poll(10, TimeUnit.MILLISECONDS);
        }
    }
}
