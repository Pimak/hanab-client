package com.pimak.hanab.client.protocol;

/** Checked failure that identifies whether a frame was malformed or semantically unsupported. */
public final class ProtocolDecodeException extends Exception {
    public enum Code { INVALID_JSON, WRONG_SHAPE, UNKNOWN_FIELD, UNKNOWN_COMMAND, INVALID_VALUE }
    private final Code code;
    public ProtocolDecodeException(Code code, String message) { super(message); this.code = code; }
    public ProtocolDecodeException(Code code, String message, Throwable cause) { super(message, cause); this.code = code; }
    public Code code() { return code; }
}
