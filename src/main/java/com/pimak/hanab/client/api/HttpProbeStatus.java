package com.pimak.hanab.client.api;

/** Outcome of an unauthenticated HTTP availability probe. */
public enum HttpProbeStatus {
    REACHABLE,
    HTTP_ERROR,
    TIMEOUT,
    CONNECTION_REFUSED,
    INVALID_OR_BLOCKED_TARGET
}
