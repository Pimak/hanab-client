package com.pimak.hanab.client.social;

/** A conversation associated with a table. */
public record TableContext(long tableId) implements SocialContext {
    public TableContext { if (tableId < 0) throw new IllegalArgumentException("tableId must be non-negative"); }
}
