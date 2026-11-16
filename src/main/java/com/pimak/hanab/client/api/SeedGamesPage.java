package com.pimak.hanab.client.api;

import com.pimak.hanab.client.catalog.GamePage;
import java.util.Objects;

/** A seed echo plus its paginated list of matching games. */
public record SeedGamesPage(String seed, GamePage page) {
    public SeedGamesPage {
        if (seed == null || seed.isBlank()) throw new IllegalArgumentException("seed must not be blank");
        Objects.requireNonNull(page, "page");
    }
}
