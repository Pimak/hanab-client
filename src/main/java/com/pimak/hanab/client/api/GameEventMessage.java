package com.pimak.hanab.client.api;

import com.pimak.hanab.client.game.GameEvent;
import com.pimak.hanab.client.model.Ids;

/** Typed envelope for a game event, preserving the table association. */
public record GameEventMessage(Ids.TableId tableId, GameEvent event) {
    public GameEventMessage {
        if (tableId == null || event == null) throw new NullPointerException();
    }
}
