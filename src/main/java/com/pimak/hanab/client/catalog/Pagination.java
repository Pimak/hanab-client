package com.pimak.hanab.client.catalog;

import java.util.List;
import java.util.Objects;

/** An immutable page of values returned by a list endpoint. */
public record Pagination<T>(List<T> values, long offset, long total) {
    public Pagination {
        values = List.copyOf(Objects.requireNonNull(values, "values"));
        if (offset < 0 || total < 0) throw new IllegalArgumentException("offset and total must be non-negative");
        if (offset > total && total != 0) throw new IllegalArgumentException("offset must not exceed total");
    }

    public List<T> items() { return values; }
    public boolean hasNext() { return offset + values.size() < total; }
}
