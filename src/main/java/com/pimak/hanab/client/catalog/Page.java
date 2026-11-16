package com.pimak.hanab.client.catalog;

import java.util.List;

/** Alias-shaped page value object for APIs that call pagination a page. */
public record Page<T>(List<T> items, long offset, long total) {
    public Page {
        Pagination<T> page = new Pagination<>(items, offset, total);
        items = page.values();
    }

    public boolean hasNext() { return offset + items.size() < total; }
}
