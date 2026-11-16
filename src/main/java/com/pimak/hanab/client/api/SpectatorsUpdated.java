package com.pimak.hanab.client.api;

import com.pimak.hanab.client.game.Spectator;
import com.pimak.hanab.client.model.Ids;
import java.util.List;

/** Complete spectator roster for a table. */
public record SpectatorsUpdated(Ids.TableId tableId, List<Spectator> spectators) {
    public SpectatorsUpdated { if (tableId == null || spectators == null) throw new NullPointerException(); spectators = List.copyOf(spectators); }
}
