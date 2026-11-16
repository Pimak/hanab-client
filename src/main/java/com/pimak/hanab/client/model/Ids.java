package com.pimak.hanab.client.model;

/** Validated identifiers used by the Hanab protocol. */
public final class Ids {
    private Ids() { }
    public record UserId(long value) { public UserId { if (value < 1 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("user id"); } }
    /** Hanab Live wire TableId: unsigned safe JSON integer, including the synthetic table 0 used by fixtures. */
    public record TableId(long value) { public TableId { if (value < 0 || value > 9_007_199_254_740_991L) throw new IllegalArgumentException("table id"); } }
    public record GameId(long value) { public GameId { if (value < 1 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("game id"); } }
    public record Username(String value) { public Username { if (value == null || value.isBlank()) throw new IllegalArgumentException("username"); } }
    public record CardOrder(int value) { public CardOrder { if (value < 0 || value > 65) throw new IllegalArgumentException("card order"); } }
    public record Turn(int value) { public Turn { if (value < 0 || value > 512) throw new IllegalArgumentException("turn"); } }
}
