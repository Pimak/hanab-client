package com.pimak.hanab.client.model;

/** Server-compatible time control for a table. */
public record TimeControl(int baseSeconds, int perTurnSeconds) {
    public TimeControl {
        if (baseSeconds < 0 || baseSeconds > 604_800 || perTurnSeconds < 0 || perTurnSeconds > 86_400)
            throw new IllegalArgumentException("time control is out of range");
    }
}
