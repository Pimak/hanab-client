package com.pimak.hanab.client.social;

/** A conversation associated with a game at a table. */
public record GameContext(long tableId, long gameId) implements SocialContext {
    public GameContext {
        if (tableId < 0) throw new IllegalArgumentException("tableId must be non-negative");
        if (gameId < 0) throw new IllegalArgumentException("gameId must be non-negative");
    }
}
