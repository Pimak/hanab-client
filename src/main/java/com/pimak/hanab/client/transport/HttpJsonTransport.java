package com.pimak.hanab.client.transport;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.nio.ByteBuffer;
import java.io.ByteArrayOutputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.pimak.hanab.client.api.SeedGamesPage;
import com.pimak.hanab.client.catalog.GamePage;
import com.pimak.hanab.client.catalog.GameHistoryEntry;
import com.pimak.hanab.client.catalog.HistoryQuery;
import com.pimak.hanab.client.catalog.VariantCatalog;
import com.pimak.hanab.client.model.ReplayDocument;
import com.pimak.hanab.client.replay.ReplayDocumentCodec;

/** Framework-neutral HTTP JSON access to the Hanab public endpoints. */
public final class HttpJsonTransport implements AutoCloseable {
    private static final int MAX_RESPONSE_BYTES = 1_048_576;
    private final URI baseUri;
    private final HttpClient client;
    private final Duration timeout;
    private volatile String cookie;
    private volatile boolean closed;
    private final java.util.concurrent.atomic.AtomicLong sessionGeneration = new java.util.concurrent.atomic.AtomicLong();
    private final Set<CompletableFuture<?>> inFlight = ConcurrentHashMap.newKeySet();
    private final ObjectMapper mapper = new ObjectMapper();

    public HttpJsonTransport(URI baseUri) { this(baseUri, Duration.ofSeconds(10)); }

    public HttpJsonTransport(URI baseUri, Duration timeout) {
        this(baseUri, timeout, HttpClient.newBuilder().connectTimeout(timeout).build());
    }

    public HttpJsonTransport(URI baseUri, Duration timeout, HttpClient client) {
        this.baseUri = Objects.requireNonNull(baseUri).toString().endsWith("/") ? baseUri : URI.create(baseUri + "/");
        if (this.baseUri.getUserInfo() != null) throw new IllegalArgumentException("base URI must not contain user information");
        if (!"https".equals(this.baseUri.getScheme()) && !isLoopback(this.baseUri.getHost())) throw new IllegalArgumentException("HTTP is only allowed for a loopback test server");
        this.timeout = Objects.requireNonNull(timeout);
        this.client = Objects.requireNonNull(client);
    }
    private static boolean isLoopback(String host) { return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host); }

    public CompletableFuture<Map<String, Object>> login(String username, String password) { return login(username, password, false); }
    public CompletableFuture<Map<String, Object>> login(String username, String password, boolean createAccount) {
        Map<String,String> form = new LinkedHashMap<>(); form.put("username", username); form.put("password", password); form.put("client", "bot"); form.put("createAccount", "false");
        form.put("createAccount", Boolean.toString(createAccount));
        return mapCancellable(requestForm("POST", "/login", form), this::asObject);
    }

    public CompletableFuture<Map<String, Object>> identity() { return get("/api/v1/identity/token"); }
    /** Resolves an identity token through the public identity endpoint. */
    public CompletableFuture<Map<String, Object>> identity(String token) {
        if (token == null || token.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("token must not be blank"));
        return mapCancellable(request("POST", "/api/v1/identity", Map.of("token", token)), this::asObject);
    }
    /** Hashes a password for the administrator-only password reset flow; callers should clear their input promptly. */
    public CompletableFuture<String> passwordHash(String password) {
        if (password == null || password.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("password must not be blank"));
        return mapCancellable(request("POST", "/password-reset", Map.of("password", password)), value -> {
            Map<String, Object> object = asObject(value);
            Object hash = object.get("hash");
            if (!(hash instanceof String text) || text.isBlank()) throw new TransportException("Password hash response is missing hash");
            return text;
        });
    }
    /** Same administrator-only operation without retaining the caller's mutable secret. */
    public CompletableFuture<String> passwordHash(char[] password) {
        if (password == null || password.length == 0) return CompletableFuture.failedFuture(new IllegalArgumentException("password must not be blank"));
        char[] copy = password.clone();
        try { return passwordHash(new String(copy)); }
        finally { java.util.Arrays.fill(copy, '\0'); }
    }
    public CompletableFuture<Map<String, Object>> catalog() { return get("/api/v1/variants"); }
    public CompletableFuture<Map<String, Object>> catalogFull() { return get("/api/v1/variants-full"); }
    /** Loads the public variant catalogue into its immutable typed projection. */
    public CompletableFuture<VariantCatalog> variantCatalog() { return VariantCatalog.load(this); }
    /** Loads a typed, filtered page of a player's history from the public HTTP API. */
    public CompletableFuture<GamePage> history(java.util.List<String> players, HistoryQuery query) {
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(query, "query");
        if (players.isEmpty() || players.size() > 6 || players.stream().anyMatch(p -> p == null || p.isBlank()))
            return CompletableFuture.failedFuture(new IllegalArgumentException("one to six players are required"));
        String path = "/api/v1/history/" + players.stream().map(HttpJsonTransport::encodePath).collect(java.util.stream.Collectors.joining("/"));
        return mapCancellable(get(path + queryString(query)), value -> GamePage.from(mapper.valueToTree(value)));
    }
    /** Loads the unpaged history export, preserving the server's complete game entries. */
    public CompletableFuture<java.util.List<GameHistoryEntry>> historyFull(java.util.List<String> players, String start, String end) {
        Objects.requireNonNull(players, "players");
        if (players.isEmpty() || players.size() > 6 || players.stream().anyMatch(p -> p == null || p.isBlank()))
            return CompletableFuture.failedFuture(new IllegalArgumentException("one to six players are required"));
        String path = "/api/v1/history-full/" + players.stream().map(HttpJsonTransport::encodePath).collect(java.util.stream.Collectors.joining("/"));
        StringBuilder query = new StringBuilder();
        if (start != null) query.append("?start=").append(encodeForm(start));
        if (end != null) query.append(query.length() == 0 ? "?end=" : "&end=").append(encodeForm(end));
        return mapCancellable(request("GET", path + query, null), value -> gameEntries(value, "Expected JSON game list"));
    }
    public CompletableFuture<SeedGamesPage> seedGames(com.pimak.hanab.client.catalog.SeedGamesQuery query) {
        Objects.requireNonNull(query, "query");
        HistoryQuery page = new HistoryQuery(query.offset(), query.limit());
        return mapCancellable(get("/api/v1/seed/" + encodePath(query.seed()) + queryString(page)),
                value -> new SeedGamesPage(query.seed(), GamePage.from(mapper.valueToTree(value))));
    }
    /** Loads the unpaged history export for a seed. */
    public CompletableFuture<java.util.List<GameHistoryEntry>> seedGamesFull(String seed, int offset, int limit) {
        if (seed == null || seed.isBlank() || offset < 0 || limit < 1 || limit > 100)
            return CompletableFuture.failedFuture(new IllegalArgumentException("invalid seed page"));
        return mapCancellable(request("GET", "/api/v1/seed-full/" + encodePath(seed) + "?offset=" + offset + "&limit=" + limit, null),
                value -> gameEntries(value, "Expected JSON game list"));
    }
    /** Lists games tagged by a player, as returned by /api/v1/tags/{player}. */
    public CompletableFuture<java.util.List<GameHistoryEntry>> tagsForPlayer(String player) {
        if (player == null || player.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("player must not be blank"));
        return mapCancellable(request("GET", "/api/v1/tags/" + encodePath(player), null),
                value -> gameEntries(value, "Expected JSON game list"));
    }
    /** Loads all games carrying a tag; the server returns the unpaged tag result. */
    public CompletableFuture<java.util.List<Map<String, Object>>> taggedGames(String tag) {
        if (tag == null || tag.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("tag must not be blank"));
        return mapCancellable(request("GET", "/api/v1/tag/" + encodePath(tag), null), value -> {
            if (!(value instanceof java.util.List<?> list)) throw new TransportException("Expected JSON game list");
            java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object item : list) {
                if (!(item instanceof Map<?, ?> map)) throw new TransportException("Expected JSON game object");
                @SuppressWarnings("unchecked") Map<String, Object> object = (Map<String, Object>) map;
                result.add(java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(object)));
            }
            return java.util.List.copyOf(result);
        });
    }
    /** Loads tag results with the same typed contract as history pages. */
    public CompletableFuture<java.util.List<GameHistoryEntry>> taggedGameEntries(String tag) {
        if (tag == null || tag.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("tag must not be blank"));
        return mapCancellable(request("GET", "/api/v1/tag/" + encodePath(tag), null), value -> {
            JsonNode array = mapper.valueToTree(value);
            if (!array.isArray()) throw new TransportException("Expected JSON game list");
            var result = new java.util.ArrayList<GameHistoryEntry>();
            array.forEach(item -> result.add(GameHistoryEntry.from(item)));
            return java.util.List.copyOf(result);
        });
    }
    public CompletableFuture<Map<String, Object>> replay(String replayId) {
        if (replayId == null || !replayId.matches("[1-9][0-9]*"))
            return CompletableFuture.failedFuture(new IllegalArgumentException("replayId must be a positive numeric id"));
        return get("/export/" + replayId);
    }
    public CompletableFuture<Map<String, Object>> replayDocument(String replayId) {
        if (replayId == null || !replayId.matches("[1-9][0-9]*")) return CompletableFuture.failedFuture(new IllegalArgumentException("replayId must be a positive numeric id"));
        return get("/export/" + replayId);
    }
    public CompletableFuture<ReplayDocument> replayDocumentDecoded(String replayId) {
        return mapCancellable(replayDocument(replayId), value -> {
            try { return ReplayDocumentCodec.importJson(Json.stringify(value)); }
            catch (Exception error) { throw new TransportException("Invalid replay document", error); }
        });
    }

    public CompletableFuture<Map<String, Object>> get(String path) { return mapCancellable(request("GET", path, null), this::asObject); }
    public CompletableFuture<Void> logout() { return mapCancellable(request("GET", "/logout", null), ignored -> { clearSession(); return null; }); }
    public CompletableFuture<Void> testCookie() { return mapCancellable(request("GET", "/test-cookie", null), ignored -> null); }

    public CompletableFuture<Object> request(String method, String path, Map<String, ?> body) {
        if (closed) return CompletableFuture.failedFuture(new TransportException("HTTP transport is closed"));
        long requestGeneration = sessionGeneration.get();
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(resolve(path)).timeout(timeout)
                    .header("Accept", "application/json");
            if (cookie != null) b.header("Cookie", cookie);
            if (body == null) b.method(method, HttpRequest.BodyPublishers.noBody());
            else b.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(Json.stringify(body)));
            CompletableFuture<HttpResponse<String>> network = client.sendAsync(b.build(), boundedBodyHandler()).toCompletableFuture();
            inFlight.add(network);
            CompletableFuture<Object> result = new CompletableFuture<>();
            network.thenCompose(r -> {
                if (r.statusCode() < 200 || r.statusCode() >= 300) return CompletableFuture.failedFuture(httpError(r));
                if ("POST".equals(method) && requestGeneration == sessionGeneration.get() && !closed) updateCookies(r.headers());
                try { return CompletableFuture.completedFuture(Json.parse(r.body())); }
                catch (RuntimeException e) { return CompletableFuture.failedFuture(new TransportException("Invalid JSON response", e)); }
            }).whenComplete((value, error) -> {
                inFlight.remove(network);
                if (error == null) result.complete(value); else result.completeExceptionally(unwrap(error));
            });
            result.whenComplete((value, error) -> { if (result.isCancelled()) network.cancel(true); });
            return result;
        } catch (RuntimeException e) { return CompletableFuture.failedFuture(new TransportException("Invalid HTTP request", e)); }
    }

    private CompletableFuture<Object> requestForm(String method, String path, Map<String, String> form) {
        if (closed) return CompletableFuture.failedFuture(new TransportException("HTTP transport is closed"));
        long requestGeneration = sessionGeneration.get();
        try {
            String encoded = form.entrySet().stream().map(entry -> encodeForm(entry.getKey()) + "=" + encodeForm(entry.getValue())).collect(java.util.stream.Collectors.joining("&"));
            HttpRequest.Builder b = HttpRequest.newBuilder(resolve(path)).timeout(timeout).header("Accept", "application/json").header("Content-Type", "application/x-www-form-urlencoded");
            if (cookie != null) b.header("Cookie", cookie);
            HttpRequest request = b.method(method, HttpRequest.BodyPublishers.ofString(encoded)).build();
            CompletableFuture<HttpResponse<String>> network = client.sendAsync(request, boundedBodyHandler()).toCompletableFuture();
            inFlight.add(network);
            CompletableFuture<Object> result = new CompletableFuture<>();
            network.thenCompose(response -> parseResponse(method, response, requestGeneration)).whenComplete((value, error) -> {
                inFlight.remove(network);
                if (error == null) result.complete(value); else result.completeExceptionally(unwrap(error));
            });
            result.whenComplete((value, error) -> { if (result.isCancelled()) network.cancel(true); });
            return result;
        } catch (RuntimeException e) { return CompletableFuture.failedFuture(new TransportException("Invalid HTTP request", e)); }
    }
    private static HttpResponse.BodyHandler<String> boundedBodyHandler() {
        return info -> new BoundedBodySubscriber(MAX_RESPONSE_BYTES);
    }
    private static final class BoundedBodySubscriber implements HttpResponse.BodySubscriber<String> {
        private final int limit; private final CompletableFuture<String> result = new CompletableFuture<>(); private final ByteArrayOutputStream bytes = new ByteArrayOutputStream(); private Flow.Subscription subscription; private int size;
        private BoundedBodySubscriber(int limit) { this.limit = limit; }
        @Override public CompletionStage<String> getBody() { return result; }
        @Override public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; subscription.request(1); }
        @Override public void onNext(List<ByteBuffer> buffers) { if (result.isDone()) return; for (ByteBuffer buffer : buffers) { if (buffer.remaining() > limit - size) { subscription.cancel(); result.completeExceptionally(new TransportException("HTTP response too large")); return; } var copy = new byte[buffer.remaining()]; buffer.get(copy); bytes.writeBytes(copy); size += copy.length; } subscription.request(1); }
        @Override public void onError(Throwable error) { result.completeExceptionally(error); }
        @Override public void onComplete() { if (!result.isDone()) result.complete(new String(bytes.toByteArray(), java.nio.charset.StandardCharsets.UTF_8)); }
    }

    private CompletableFuture<Object> parseResponse(String method, HttpResponse<String> response, long requestGeneration) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) return CompletableFuture.failedFuture(httpError(response));
        if (requestGeneration == sessionGeneration.get() && !closed) updateCookies(response.headers());
        try { return CompletableFuture.completedFuture(Json.parse(response.body())); }
        catch (RuntimeException e) { return CompletableFuture.failedFuture(new TransportException("Invalid JSON response", e)); }
    }

    private static String encodeForm(String value) { return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8); }
    private static String queryString(HistoryQuery query) {
        StringBuilder result = new StringBuilder("?offset=").append(query.offset()).append("&limit=").append(query.limit());
        if (query.sort() != null) result.append("&sort=").append(encodeForm(query.sort()));
        if (query.order() != null) result.append("&order=").append(query.order().name().toLowerCase(java.util.Locale.ROOT));
        query.filters().forEach((key, value) -> result.append("&filter%5B").append(encodeForm(key)).append("%5D=").append(value));
        return result.toString();
    }

    public void clearSession() { sessionGeneration.incrementAndGet(); cookie = null; }
    public boolean hasSession() { return cookie != null; }
    @Override public void close() { closed = true; sessionGeneration.incrementAndGet(); inFlight.forEach(request -> request.cancel(true)); inFlight.clear(); clearSession(); }

    private URI resolve(String path) {
        if (path == null || path.isBlank() || path.contains("\\") || path.startsWith("//")) throw new IllegalArgumentException("path must be a relative HTTP path");
        URI candidate = baseUri.resolve(path.startsWith("/") ? path.substring(1) : path);
        if (!Objects.equals(candidate.getScheme(), baseUri.getScheme()) || !Objects.equals(candidate.getAuthority(), baseUri.getAuthority())) throw new IllegalArgumentException("path escapes configured HTTP origin");
        return candidate;
    }
    private static String encodePath(String value) {
        StringBuilder b = new StringBuilder();
        for (byte c : value.getBytes(java.nio.charset.StandardCharsets.UTF_8)) {
            int n = c & 0xff;
            if ((n >= 'a' && n <= 'z') || (n >= 'A' && n <= 'Z') || (n >= '0' && n <= '9') || "-._~".indexOf(n) >= 0) b.append((char)n);
            else b.append('%').append(String.format("%02X", n));
        }
        return b.toString();
    }
    @SuppressWarnings("unchecked") private Map<String,Object> asObject(Object v) { if (!(v instanceof Map<?,?>)) throw new TransportException("Expected JSON object"); return (Map<String,Object>) v; }
    private TransportException httpError(HttpResponse<String> response) {
        String message = "HTTP request failed: " + response.statusCode(); String code = null; String diagnosticId = null;
        try { JsonNode body = mapper.readTree(response.body()); if (body != null && body.isObject()) { if (body.path("message").isTextual()) message = body.path("message").textValue(); if (body.path("code").isTextual()) code = body.path("code").textValue(); if (body.path("diagnosticId").isTextual()) diagnosticId = body.path("diagnosticId").textValue(); } } catch (Exception ignored) { }
        return new TransportException(message, response.statusCode(), code, diagnosticId);
    }
    private static Throwable unwrap(Throwable error) { return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error; }
    private synchronized void updateCookies(HttpHeaders headers) {
        var values = headers.allValues("set-cookie");
        if (!values.isEmpty()) {
            var current = new LinkedHashMap<String, String>();
            if (cookie != null) for (String part : cookie.split(";\\s*")) { int split = part.indexOf('='); if (split > 0) current.put(part.substring(0, split), part.substring(split + 1)); }
            for (String value : values) { String pair = value.split(";", 2)[0]; int split = pair.indexOf('='); if (split > 0) current.put(pair.substring(0, split), pair.substring(split + 1)); }
            cookie = current.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).collect(java.util.stream.Collectors.joining("; "));
        }
    }
    private java.util.List<GameHistoryEntry> gameEntries(Object value, String message) {
        JsonNode array = mapper.valueToTree(value);
        if (!array.isArray()) throw new TransportException(message);
        var result = new java.util.ArrayList<GameHistoryEntry>(); array.forEach(item -> result.add(GameHistoryEntry.from(item)));
        return java.util.List.copyOf(result);
    }
    private static <T, R> CompletableFuture<R> mapCancellable(CompletableFuture<T> source, java.util.function.Function<T, R> mapper) {
        var result = new CompletableFuture<R>();
        source.whenComplete((value, error) -> { if (error != null) result.completeExceptionally(error); else { try { result.complete(mapper.apply(value)); } catch (Throwable failure) { result.completeExceptionally(failure); } } });
        result.whenComplete((value, error) -> { if (result.isCancelled()) source.cancel(true); });
        return result;
    }
}
