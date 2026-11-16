package com.pimak.hanab.client.catalog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Query parameters accepted by the public history and seed endpoints. */
public record HistoryQuery(long offset, int limit, String sort, Order order, Map<String, Long> filters) {
    public enum Order { ASC, DESC }

    public HistoryQuery {
        if (offset < 0 || offset > 9_007_199_254_740_991L || limit < 1 || limit > 100) throw new IllegalArgumentException("invalid history page");
        if (sort != null && !Set.of("id", "numPlayers", "score").contains(sort)) throw new IllegalArgumentException("invalid history sort");
        order = order == null ? Order.DESC : order;
        Map<String, Long> copy = new LinkedHashMap<>();
        if (filters != null) filters.forEach((key, value) -> {
            if (key == null || !Set.of("id", "numPlayers", "score", "variant").contains(key)) throw new IllegalArgumentException("invalid history filter");
            copy.put(key, Objects.requireNonNull(value, "filter value"));
        });
        filters = Map.copyOf(copy);
    }

    public HistoryQuery(int offset, int limit) { this(offset, limit, null, Order.DESC, Map.of()); }
    public static HistoryQuery firstPage() { return new HistoryQuery(0, 50); }
}
