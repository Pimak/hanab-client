package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pimak.hanab.client.model.Ids;

/** Source of a replay table, matching the bot protocol union. */
public sealed interface ReplaySource permits ReplaySource.Saved, ReplaySource.Imported {
    ObjectNode toJson(ObjectMapper mapper);

    record Saved(Ids.GameId gameId) implements ReplaySource {
        public Saved { if (gameId == null) throw new NullPointerException("gameId"); }
        @Override public ObjectNode toJson(ObjectMapper mapper) { return mapper.createObjectNode().put("type", "saved").put("gameId", gameId.value()); }
    }

    record Imported(JsonNode game) implements ReplaySource {
        public Imported { if (game == null || !game.isObject()) throw new IllegalArgumentException("game must be an object"); game = game.deepCopy(); }
        @Override public JsonNode game() { return game.deepCopy(); }
        @Override public ObjectNode toJson(ObjectMapper mapper) { return mapper.createObjectNode().put("type", "imported").set("game", game.deepCopy()); }
    }
}
