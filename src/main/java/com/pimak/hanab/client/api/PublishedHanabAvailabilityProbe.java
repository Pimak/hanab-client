package com.pimak.hanab.client.api;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Safe, unauthenticated HTTP/1.1 availability probe for a Hanab origin.
 * It always requests {@code GET /}, does not follow redirects, and never
 * interprets the response body as JSON.
 */
public final class PublishedHanabAvailabilityProbe {
    private final HttpClient client;
    private final Duration timeout;

    public PublishedHanabAvailabilityProbe() {
        this(Duration.ofSeconds(10));
    }

    public PublishedHanabAvailabilityProbe(Duration timeout) {
        this(timeout, HttpClient.newBuilder().connectTimeout(validTimeout(timeout)).version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER).build());
    }

    public PublishedHanabAvailabilityProbe(Duration timeout, HttpClient client) {
        this.timeout = validTimeout(timeout);
        this.client = java.util.Objects.requireNonNull(client, "client");
    }

    public CompletionStage<HttpProbeResult> probe(URI target) {
        final URI origin;
        try {
            origin = PublishedHanabHttpSupport.origin(target);
        } catch (IllegalArgumentException error) {
            return CompletableFuture.completedFuture(new HttpProbeResult(HttpProbeStatus.INVALID_OR_BLOCKED_TARGET, OptionalInt.empty()));
        }
        HttpRequest request = HttpRequest.newBuilder(origin)
                .GET().timeout(timeout).version(HttpClient.Version.HTTP_1_1).header("Accept", "*/*").build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .handle((response, error) -> {
                    if (error == null) {
                        int status = response.statusCode();
                        return new HttpProbeResult(status < 400 ? HttpProbeStatus.REACHABLE : HttpProbeStatus.HTTP_ERROR, OptionalInt.of(status));
                    }
                    Throwable cause = PublishedHanabHttpSupport.unwrap(error);
                    if (cause instanceof HttpTimeoutException) return new HttpProbeResult(HttpProbeStatus.TIMEOUT, OptionalInt.empty());
                    if (cause instanceof ConnectException) return new HttpProbeResult(HttpProbeStatus.CONNECTION_REFUSED, OptionalInt.empty());
                    return new HttpProbeResult(HttpProbeStatus.CONNECTION_REFUSED, OptionalInt.empty());
                });
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
