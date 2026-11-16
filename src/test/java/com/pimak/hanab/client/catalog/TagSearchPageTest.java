package com.pimak.hanab.client.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TagSearchPageTest {
    private static final String GAME = "{\"gameId\":1,\"options\":{},\"seed\":\"s\",\"score\":25,\"numTurns\":10,\"endCondition\":\"completed\",\"startedAt\":\"2026-01-01T00:00:00Z\",\"finishedAt\":\"2026-01-01T00:01:00+00:00\",\"numGamesOnSeed\":1,\"playerNames\":[\"bot\"],\"tags\":[\"training\"]}";
    @Test void decodesLegacyArrayAsFirstPage() throws Exception {
        var page = TagSearchPage.from(new ObjectMapper().readTree("[" + GAME + "]"));
        assertEquals(1, page.total()); assertFalse(page.hasNext());
    }
}
