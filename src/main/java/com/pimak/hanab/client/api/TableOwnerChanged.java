package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.Ids;
import com.pimak.hanab.client.model.User;

/** Typed table-owner transition, also used as the shared-replay leader transition. */
public record TableOwnerChanged(Ids.TableId tableId, User owner) {
    public TableOwnerChanged { if (tableId == null || owner == null) throw new NullPointerException(); }
}
