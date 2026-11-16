package com.pimak.hanab.client.api;
import com.fasterxml.jackson.databind.JsonNode;
import com.pimak.hanab.client.model.Ids;
import java.util.Optional;
public record GameFinished(Ids.TableId tableId, Optional<Ids.GameId> gameId, JsonNode sharedReplay) {
    public GameFinished { if (tableId == null || gameId == null || sharedReplay == null || !sharedReplay.isObject()) throw new IllegalArgumentException("invalid gameFinished event"); sharedReplay = sharedReplay.deepCopy(); }
    @Override public JsonNode sharedReplay() { return sharedReplay.deepCopy(); }
}
