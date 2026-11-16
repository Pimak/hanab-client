package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Visibility policy for a replay table, matching the bot protocol union. */
public sealed interface ReplayVisibility permits ReplayVisibility.Solo, ReplayVisibility.Shared {
    ObjectNode toJson(ObjectMapper mapper);

    record Solo() implements ReplayVisibility {
        @Override public ObjectNode toJson(ObjectMapper mapper) { return mapper.createObjectNode().put("type", "solo"); }
    }

    record Shared(boolean joinExisting, boolean spoilerFree) implements ReplayVisibility {
        @Override public ObjectNode toJson(ObjectMapper mapper) {
            return mapper.createObjectNode().put("type", "shared")
                    .put("joinExisting", joinExisting).put("spoilerFree", spoilerFree);
        }
    }
}
