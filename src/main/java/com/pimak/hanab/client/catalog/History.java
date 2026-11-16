package com.pimak.hanab.client.catalog;

import java.util.List;
import java.util.Objects;

/** An immutable history list, optionally carrying the server page metadata. */
public record History<T>(List<T> entries, Pagination<T> page) {
    public History {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public History(List<T> entries) { this(entries, null); }
    public List<T> values() { return entries; }
}
