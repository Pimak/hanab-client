package com.pimak.hanab.client.transport;

/** A failure at the HTTP or WebSocket transport boundary. */
public class TransportException extends com.pimak.hanab.client.api.TransportException {

    public TransportException(String message) {
        super(message);
    }

    public TransportException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransportException(String message, int statusCode) {
        super(message, statusCode);
    }

    public TransportException(String message, Throwable cause, int statusCode) {
        super(message, statusCode, cause);
    }
    public TransportException(String message, int statusCode, String code, String diagnosticId) {
        super(message, statusCode, code, diagnosticId);
    }
}
