package com.pimak.hanab.client.api;

public record Credentials(String username, char[] password) implements AutoCloseable {
    public Credentials { if (username == null || username.isBlank() || password == null || password.length == 0) throw new IllegalArgumentException("credentials"); password = password.clone(); }
    public char[] passwordCopy() { return password.clone(); }
    @Override public char[] password() { return password.clone(); }
    @Override public void close() { java.util.Arrays.fill(password, '\0'); }
}
