package com.pimak.hanab.client.catalog;

import java.net.URI;
import java.util.Objects;

/** A typed value addressed by an immutable share link. */
public record ShareLink<T>(URI uri, T value) {
    public ShareLink {
        uri = Objects.requireNonNull(uri, "uri");
        value = Objects.requireNonNull(value, "value");
    }
}
