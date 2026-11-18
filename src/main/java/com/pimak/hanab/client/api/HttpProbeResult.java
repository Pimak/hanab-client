package com.pimak.hanab.client.api;

import java.util.Objects;
import java.util.OptionalInt;

/**
 * Result of an HTTP availability probe. A status code is present whenever the
 * remote server produced an HTTP response, including an error response.
 */
public record HttpProbeResult(HttpProbeStatus status, OptionalInt httpStatus) {
    public HttpProbeResult {
        status = Objects.requireNonNull(status, "status");
        httpStatus = Objects.requireNonNull(httpStatus, "httpStatus");
        if (httpStatus.isPresent() && (httpStatus.getAsInt() < 100 || httpStatus.getAsInt() > 599)) {
            throw new IllegalArgumentException("invalid HTTP status");
        }
    }

    public boolean reachable() {
        return status == HttpProbeStatus.REACHABLE;
    }
}
