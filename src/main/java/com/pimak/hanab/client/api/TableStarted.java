package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.Ids;

/** Typed lifecycle transition emitted when a table enters a game or replay. */
public record TableStarted(Ids.TableId tableId, Stage stage) {
    public TableStarted { if (tableId == null || stage == null) throw new NullPointerException(); }
    public enum Stage { JUST_STARTED, PLAYING, REPLAY }
}
