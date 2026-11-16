package com.pimak.hanab.client.protocol;

/** Invalid payload data from the current Hanab protocol. */
public final class ProtocolDecodeException extends Exception {
    public enum Code { INVALID_JSON, WRONG_SHAPE, INVALID_VALUE }
    private final Code code;
    public ProtocolDecodeException(Code code, String message) { super(message); this.code = code; }
    public ProtocolDecodeException(Code code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public Code code() { return code; }
}
