package com.pimak.hanab.client.api;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import org.junit.jupiter.api.Test;

class BotProtocolTest {
    @Test void botConfigurationDoesNotCarryWebVersion() {
        var config = ClientConfig.bot(URI.create("http://localhost:1212"));
        assertTrue(LoginOptions.bot(true).createAccount());
    }
}
