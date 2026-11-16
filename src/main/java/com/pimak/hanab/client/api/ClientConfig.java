package com.pimak.hanab.client.api;

import java.net.URI;
import java.time.Duration;

public record ClientConfig(URI baseUri, String clientKind, String version, Duration requestTimeout,
                           Duration reconnectInitial, Duration reconnectMax) {
    public ClientConfig {
        if (baseUri == null || clientKind == null || requestTimeout == null || reconnectInitial == null || reconnectMax == null) throw new NullPointerException();
        if (baseUri.getUserInfo() != null) throw new IllegalArgumentException("base URI must not contain user information");
        if (!"http".equals(baseUri.getScheme()) && !"https".equals(baseUri.getScheme())) throw new IllegalArgumentException("base URI must be HTTP(S)");
        if ("http".equals(baseUri.getScheme()) && !isLoopback(baseUri.getHost())) throw new IllegalArgumentException("HTTP is only allowed for a loopback test server");
        try { if (requestTimeout.toMillis() < 1 || reconnectInitial.toMillis() < 1 || reconnectMax.toMillis() < 1) throw new IllegalArgumentException("durations must be at least 1ms"); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("durations are too large", e); }
        if (requestTimeout.isNegative() || requestTimeout.isZero() || reconnectInitial.isNegative() || reconnectInitial.isZero()
                || reconnectMax.isNegative() || reconnectMax.isZero() || reconnectInitial.compareTo(reconnectMax) > 0) throw new IllegalArgumentException("durations must be positive and ordered");
        String path = baseUri.getPath();
        if (path == null || path.isBlank()) path = "/";
        else if (!path.endsWith("/")) path += "/";
        try { baseUri = new URI(baseUri.getScheme(), baseUri.getUserInfo(), baseUri.getHost(), baseUri.getPort(), path, baseUri.getQuery(), baseUri.getFragment()); }
        catch (java.net.URISyntaxException e) { throw new IllegalArgumentException("invalid base URI", e); }
    }
    private static boolean isLoopback(String host) { return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host); }
    public static ClientConfig of(URI baseUri) { return new ClientConfig(baseUri, "app", null, Duration.ofSeconds(15), Duration.ofMillis(250), Duration.ofSeconds(10)); }
    /** Configuration for the Hanab bot protocol: no web release version is sent. */
    public static ClientConfig bot(URI baseUri) { return new ClientConfig(baseUri, "bot", null, Duration.ofSeconds(15), Duration.ofMillis(250), Duration.ofSeconds(10)); }
}
