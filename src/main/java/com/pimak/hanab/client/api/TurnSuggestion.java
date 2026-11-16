package com.pimak.hanab.client.api;
import com.pimak.hanab.client.model.Ids;
public record TurnSuggestion(Ids.TableId tableId, String username, int turn) {
    public TurnSuggestion { if (tableId == null || username == null || username.isBlank() || turn < 0 || turn > 512) throw new IllegalArgumentException("invalid turn suggestion"); }
}
