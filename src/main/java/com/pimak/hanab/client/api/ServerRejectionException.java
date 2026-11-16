package com.pimak.hanab.client.api;

/** A command was received by the server but refused as a protocol or permission error. */
public final class ServerRejectionException extends RuntimeException {
    private final String command;
    private final String code;
    private final String diagnosticId;

    public ServerRejectionException(String command, String message, String code, String diagnosticId) {
        super(message);
        this.command = command;
        this.code = code;
        this.diagnosticId = diagnosticId;
    }
    public String command() { return command; }
    public String code() { return code; }
    public String diagnosticId() { return diagnosticId; }
}
