package com.pimak.hanab.client.api;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

final class PublishedHanabHttpSupport {
    static final int MAX_RESPONSE_BYTES = 1_048_576;

    private PublishedHanabHttpSupport() { }

    static URI origin(URI target) {
        if (target == null || !target.isAbsolute() || target.getUserInfo() != null || target.getHost() == null
                || target.getHost().isBlank() || target.getQuery() != null || target.getFragment() != null) {
            throw new IllegalArgumentException("target must be an absolute HTTP(S) origin without credentials");
        }
        String scheme = target.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("target must use HTTP(S)");
        }
        int port = target.getPort();
        if (port == 0 || port < -1 || port > 65535) {
            throw new IllegalArgumentException("target has an invalid port");
        }
        if (scheme.equals("http") && !isLoopback(target.getHost())) {
            throw new IllegalArgumentException("HTTP targets must be loopback addresses");
        }
        if (isPrivateLiteralAddress(target.getHost()) && !isLoopback(target.getHost())) {
            throw new IllegalArgumentException("private network targets are blocked");
        }
        try {
            return new URI(scheme, null, target.getHost(), port, "/", null, null);
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException("invalid target", error);
        }
    }

    static HttpResponse.BodyHandler<String> boundedTextBodyHandler() {
        return ignored -> new BoundedTextSubscriber(MAX_RESPONSE_BYTES);
    }

    static Throwable unwrap(Throwable error) {
        while ((error instanceof java.util.concurrent.CompletionException || error instanceof java.util.concurrent.ExecutionException)
                && error.getCause() != null) {
            error = error.getCause();
        }
        return error;
    }

    private static boolean isLoopback(String host) {
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private static boolean isPrivateLiteralAddress(String host) {
        if (!(host.matches("[0-9.]+") || host.indexOf(':') >= 0)) return false;
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isAnyLocalAddress() || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                    || address.isLoopbackAddress();
        } catch (Exception ignored) {
            return true;
        }
    }

    private static final class BoundedTextSubscriber implements HttpResponse.BodySubscriber<String> {
        private final CompletableFuture<String> body = new CompletableFuture<>();
        private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        private final int limit;
        private Flow.Subscription subscription;
        private int size;

        private BoundedTextSubscriber(int limit) {
            this.limit = limit;
        }

        @Override public CompletionStage<String> getBody() { return body; }
        @Override public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; subscription.request(1); }
        @Override public void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > limit - size) {
                    subscription.cancel();
                    body.completeExceptionally(new TransportException("HTTP response exceeds " + limit + " bytes"));
                    return;
                }
                byte[] copy = new byte[buffer.remaining()];
                buffer.get(copy);
                bytes.writeBytes(copy);
                size += copy.length;
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable error) { body.completeExceptionally(error); }
        @Override public void onComplete() {
            if (!body.isDone()) body.complete(new String(bytes.toByteArray(), StandardCharsets.UTF_8));
        }
    }
}
