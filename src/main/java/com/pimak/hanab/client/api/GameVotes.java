package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.Ids;

/** End-game vote state visible to the current client. */
public record GameVotes(Ids.TableId tableId, boolean voted, int votesNeeded) {
    public GameVotes { if (tableId == null || votesNeeded < 0) throw new IllegalArgumentException("invalid game votes"); }
}
