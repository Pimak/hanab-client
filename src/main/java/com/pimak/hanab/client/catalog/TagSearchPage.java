package com.pimak.hanab.client.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Objects;

/** Typed result of a tag search, supporting both paged and legacy array responses. */
public record TagSearchPage(List<GameHistoryEntry> games, long offset, long total) {
    public TagSearchPage {
        games = List.copyOf(Objects.requireNonNull(games));
        if (offset < 0 || total < 0 || offset > total || games.size() > total - offset) throw new IllegalArgumentException("invalid tag page");
    }
    public boolean hasNext() { return offset + games.size() < total; }
    public static TagSearchPage from(JsonNode json) {
        if (json == null) throw new IllegalArgumentException("tag search response is missing");
        if (json.isArray()) { var list = new java.util.ArrayList<GameHistoryEntry>(); json.forEach(value -> list.add(GameHistoryEntry.from(value))); return new TagSearchPage(list, 0, list.size()); }
        if (!json.isObject() || !json.path("games").isArray()) throw new IllegalArgumentException("invalid tag search response");
        var list = new java.util.ArrayList<GameHistoryEntry>(); json.path("games").forEach(value -> list.add(GameHistoryEntry.from(value)));
        var offset = json.path("offset"); var total = json.path("total");
        if (!offset.isIntegralNumber() || !offset.canConvertToLong() || !total.isIntegralNumber() || !total.canConvertToLong()) throw new IllegalArgumentException("tag page is missing offset or total");
        return new TagSearchPage(list, offset.longValue(), total.longValue());
    }
}
