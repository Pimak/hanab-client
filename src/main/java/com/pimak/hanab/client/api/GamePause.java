package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.Ids;
import java.util.OptionalInt;

/** Pause state visible to the current client. */
public record GamePause(Ids.TableId tableId, OptionalInt pausingPlayerIndex, boolean queued) {
    public GamePause { if (tableId == null || pausingPlayerIndex == null) throw new NullPointerException(); }
}
