package com.pimak.hanab.client.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** A validated clue action sent to the server. */
public sealed interface Clue permits Clue.Color, Clue.Rank {
    ObjectNode toJson(ObjectMapper mapper);

    record Color(int color) implements Clue {
        public Color { if (color < 0 || color > 5) throw new IllegalArgumentException("color must be 0..5"); }
        @Override public ObjectNode toJson(ObjectMapper mapper) { return mapper.createObjectNode().put("type", "color").put("color", color); }
    }

    record Rank(int rank) implements Clue {
        public Rank { if (rank < 1 || rank > 5) throw new IllegalArgumentException("rank must be 1..5"); }
        @Override public ObjectNode toJson(ObjectMapper mapper) { return mapper.createObjectNode().put("type", "rank").put("rank", rank); }
    }
}
