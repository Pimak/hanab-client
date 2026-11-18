package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.Ids;
import com.pimak.hanab.client.model.ReplayDocument;
import com.pimak.hanab.client.replay.ReplayDocumentCodec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Fetches and decodes the public {@code /export/{gameId}} endpoint with an
 * owned HTTP/1.1 client. HTTP failures expose their status through
 * {@link TransportException#statusCode()} and response bodies are limited to 1 MiB.
 */
public final class PublishedHanabReplayFetcher {
    private final URI origin;
    private final Duration timeout;
    private final HttpClient client;

    public PublishedHanabReplayFetcher(URI target) {
        this(target, Duration.ofSeconds(10));
    }

    public PublishedHanabReplayFetcher(URI target, Duration timeout) {
        this(target, timeout, HttpClient.newBuilder().connectTimeout(validTimeout(timeout)).version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    public PublishedHanabReplayFetcher(URI target, Duration timeout, HttpClient client) {
        this.origin = PublishedHanabHttpSupport.origin(target);
        this.timeout = validTimeout(timeout);
        this.client = Objects.requireNonNull(client, "client");
    }

    public CompletionStage<ReplayDocument> fetch(Ids.GameId gameId) {
        Objects.requireNonNull(gameId, "gameId");
        URI endpoint = origin.resolve("export/" + gameId.value());
        HttpRequest request = HttpRequest.newBuilder(endpoint).GET().timeout(timeout)
                .version(HttpClient.Version.HTTP_1_1).header("Accept", "application/json").build();
        return client.sendAsync(request, PublishedHanabHttpSupport.boundedTextBodyHandler())
                .thenApply(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        throw new TransportException("Replay export failed: HTTP " + response.statusCode(), response.statusCode());
                    }
                    try {
                        return ReplayDocumentCodec.importJson(response.body());
                    } catch (Exception error) {
                        throw new TransportException("Invalid replay export", error);
                    }
                });
    }

    /** Alias for {@link #fetch(Ids.GameId)} for callers using load terminology. */
    public CompletionStage<ReplayDocument> load(Ids.GameId gameId) {
        return fetch(gameId);
    }

    private static Duration validTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        try {
            if (timeout.toMillis() < 1) throw new IllegalArgumentException("timeout must be at least 1ms");
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("timeout is too large", error);
        }
        return timeout;
    }
}
