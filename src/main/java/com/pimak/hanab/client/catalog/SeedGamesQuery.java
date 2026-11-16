package com.pimak.hanab.client.catalog;

public record SeedGamesQuery(String seed, int offset, int limit) {
    public SeedGamesQuery { if (seed == null || seed.isBlank() || seed.length() > 32 || !seed.matches("[A-Za-z0-9-]+") || offset < 0 || limit < 1 || limit > 100) throw new IllegalArgumentException("invalid seed games query"); }
}
