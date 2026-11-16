package com.pimak.hanab.client.model;

import com.fasterxml.jackson.databind.JsonNode;

public record ReplayGame(Ids.GameId gameId, JsonNode raw) {
    public ReplayGame { if (raw == null) throw new NullPointerException("raw"); raw = raw.deepCopy(); }
}
