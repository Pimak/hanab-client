package com.pimak.hanab.client.api;

import java.util.concurrent.atomic.LongAdder;

/** Low-overhead transport counters; no payloads or credentials are retained. */
public final class ClientMetrics {
    private final LongAdder commands = new LongAdder(), errors = new LongAdder(), reconnects = new LongAdder(), events = new LongAdder();
    void command() { commands.increment(); }
    void error() { errors.increment(); }
    void reconnect() { reconnects.increment(); }
    void event() { events.increment(); }
    public Snapshot snapshot() { return new Snapshot(commands.sum(), errors.sum(), reconnects.sum(), events.sum()); }
    public record Snapshot(long commands, long errors, long reconnects, long events) { }
}
