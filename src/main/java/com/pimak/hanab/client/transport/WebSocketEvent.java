package com.pimak.hanab.client.transport;

/** A lifecycle or message event emitted by {@link WebSocketTransport}. */
public record WebSocketEvent(Type type, String text, int code, String reason, Throwable error) {
    public enum Type { OPEN, HANDSHAKE_SENT, MESSAGE, CLOSED, ERROR, RECONNECTING, RECONNECTED }
    public static WebSocketEvent of(Type type) { return new WebSocketEvent(type, null, -1, null, null); }
}
