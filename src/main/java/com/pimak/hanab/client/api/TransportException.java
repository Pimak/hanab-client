package com.pimak.hanab.client.api;
public class TransportException extends RuntimeException {
    private final int statusCode;
    private final String code;
    private final String diagnosticId;
    public TransportException(String message) { this(message, -1, null); }
    public TransportException(String message, Throwable cause) { this(message, -1, cause); }
    public TransportException(String message, int statusCode) { this(message, statusCode, null); }
    public TransportException(String message, int statusCode, Throwable cause) { this(message, statusCode, cause, null, null); }
    public TransportException(String message, int statusCode, String code, String diagnosticId) { this(message, statusCode, null, code, diagnosticId); }
    public TransportException(String message, int statusCode, Throwable cause, String code, String diagnosticId) { super(message, cause); this.statusCode = statusCode; this.code = code; this.diagnosticId = diagnosticId; }
    public int statusCode() { return statusCode; }
    public String code() { return code; }
    public String diagnosticId() { return diagnosticId; }
}
