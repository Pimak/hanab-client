package com.pimak.hanab.client.social;

import com.pimak.hanab.client.model.Ids;
import java.util.Optional;

/** A non-persistent typing notification received from the server. */
public record TypingIndicator(String username, Optional<Ids.TableId> tableId, boolean typing) {
    public TypingIndicator {
        if (username == null || username.isBlank() || tableId == null) throw new NullPointerException();
        tableId = tableId.map(value -> { if (value == null) throw new NullPointerException(); return value; });
    }
}
