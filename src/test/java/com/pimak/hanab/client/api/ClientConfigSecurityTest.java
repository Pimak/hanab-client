package com.pimak.hanab.client.api;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class ClientConfigSecurityTest {
    @Test void remoteHttpIsRejectedButLoopbackHttpIsAvailableForLocalBotTests() {
        assertDoesNotThrow(() -> ClientConfig.bot(URI.create("http://localhost:1212")));
    }

    @Test void normalizesDeploymentPrefixForRelativeHttpRoutes() {
        assertEquals("/hanabi/", ClientConfig.bot(URI.create("https://example.test/hanabi")).baseUri().getPath());
    }

    @Test void closedClientRejectsASecondLoginExplicitly() {
        var client = new HanabClientImpl(ClientConfig.bot(URI.create("http://localhost:1212")));
        client.close();
        var failure = assertThrows(java.util.concurrent.CompletionException.class,
                () -> client.login(new Credentials("bot", new char[]{'x'}), LoginOptions.bot(true)).toCompletableFuture().join());
        assertInstanceOf(TransportException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("closed"));
    }
}
