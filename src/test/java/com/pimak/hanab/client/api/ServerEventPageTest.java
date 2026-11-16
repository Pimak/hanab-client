package com.pimak.hanab.client.api;

import com.pimak.hanab.client.protocol.WireMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class ServerEventPageTest {
    @Test void decodesHistoryAndSeedPages() throws Exception {
        var payload = new ObjectMapper().readTree("""
          {"games":[{"gameId":7,"options":{"variant":"No Variant"},"seed":"abc","score":25,"numTurns":40,"endCondition":"deckEmpty","startedAt":"2026-01-01T00:00:00Z","finishedAt":"2026-01-01T00:01:00Z","numGamesOnSeed":2,"playerNames":["Alice"],"tags":["training"]}],"offset":0,"total":1}
          """);
        var history = new ServerEvent(Instant.now(), new WireMessage("gameHistory", payload));
        assertEquals(7, history.gamePage().orElseThrow().games().get(0).gameId());
        var seedPayload = payload.deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) seedPayload).put("seed", "abc");
        var seed = new ServerEvent(Instant.now(), new WireMessage("seedGames", seedPayload));
        assertEquals("abc", seed.seedGames().orElseThrow().seed());
    }
}
