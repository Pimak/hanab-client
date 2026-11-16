package com.pimak.hanab.client.catalog;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.OptionalInt;

/** Generates and parses stable references without coupling callers to UI routing. */
public final class ShareLinks {
    private ShareLinks() { }
    public enum Kind { TABLE, REPLAY, SHARED_REPLAY }
    public record Reference(Kind kind, String id, OptionalInt turn) {
        public Reference { Objects.requireNonNull(kind); if (id == null || id.isBlank()) throw new IllegalArgumentException("id"); Objects.requireNonNull(turn); }
    }
    public static URI table(URI origin, long id) { return build(origin, "table", id, OptionalInt.empty()); }
    public static URI replay(URI origin, long id, OptionalInt turn) { return build(origin, "replay", id, turn); }
    public static URI sharedReplay(URI origin, long id, OptionalInt turn) { return build(origin, "shared-replay", id, turn); }
    public static Reference parse(URI uri) {
        Objects.requireNonNull(uri);
        if (uri.getScheme() == null || uri.getHost() == null) throw new IllegalArgumentException("Hanab link must be an absolute URI: " + uri);
        String[] parts = uri.getPath().split("/");
        int kindIndex = -1;
        for (int i = 0; i < parts.length; i++) if (parts[i].equals("table") || parts[i].equals("replay") || parts[i].equals("shared-replay")) { kindIndex = i; break; }
        if (kindIndex < 0 || kindIndex + 2 >= parts.length || parts[kindIndex + 1].isBlank() || parts.length > kindIndex + 3 || (parts.length == kindIndex + 3 && parts[kindIndex + 2].isBlank())) throw new IllegalArgumentException("unsupported Hanab link: " + uri);
        Kind kind = switch (parts[kindIndex]) { case "table" -> Kind.TABLE; case "replay" -> Kind.REPLAY; case "shared-replay" -> Kind.SHARED_REPLAY; default -> throw new IllegalArgumentException("unsupported Hanab link: " + uri); };
        OptionalInt turn = parts.length == kindIndex + 3 ? OptionalInt.of(Integer.parseInt(parts[kindIndex + 2])) : OptionalInt.empty();
        if (turn.isPresent() && turn.getAsInt() < 0) throw new IllegalArgumentException("turn must be non-negative");
        if (kind == Kind.TABLE && turn.isPresent()) throw new IllegalArgumentException("table links do not have turns");
        if (turn.isEmpty() && uri.getQuery() != null) for (String item : uri.getQuery().split("&")) { String[] pair = item.split("=", 2); if (pair.length == 2 && pair[0].equals("turn")) turn = OptionalInt.of(Integer.parseInt(pair[1])); else if (!item.isBlank()) throw new IllegalArgumentException("unknown share-link query parameter"); }
        if (!parts[kindIndex + 1].matches("[1-9][0-9]*")) throw new IllegalArgumentException("share-link id must be positive");
        return new Reference(kind, parts[kindIndex + 1], turn);
    }
    private static URI build(URI origin, String path, long id, OptionalInt turn) {
        Objects.requireNonNull(origin, "origin"); Objects.requireNonNull(turn, "turn");
        if (id < 1 || (turn.isPresent() && turn.getAsInt() < 0)) throw new IllegalArgumentException("invalid share-link identifier");
        try { String base = origin.getPath() == null || origin.getPath().isBlank() ? "/" : (origin.getPath().endsWith("/") ? origin.getPath() : origin.getPath() + "/"); String suffix = turn.isPresent() ? "/" + turn.getAsInt() : ""; return new URI(origin.getScheme(), origin.getAuthority(), base + path + "/" + id + suffix, null, null); } catch (URISyntaxException e) { throw new IllegalArgumentException(e); }
    }
}
