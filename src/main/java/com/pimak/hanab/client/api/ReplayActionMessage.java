package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.pimak.hanab.client.model.Ids;
import com.pimak.hanab.client.replay.ReplayAction;

/** Typed replay-action envelope; the validated action union remains lossless JSON for forward compatibility. */
public record ReplayActionMessage(Ids.TableId tableId, ReplayAction action) {
    public ReplayActionMessage {
        if (tableId == null || action == null) throw new IllegalArgumentException("invalid replay action message");
    }
}
