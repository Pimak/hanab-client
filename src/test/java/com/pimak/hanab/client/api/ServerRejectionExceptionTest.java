package com.pimak.hanab.client.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServerRejectionExceptionTest {
    @Test void preservesServerErrorContext() {
        var error = new ServerRejectionException("playerAction", "Not your turn", "NotYourTurn", "diag-1");
        assertEquals("playerAction", error.command());
        assertEquals("NotYourTurn", error.code());
        assertEquals("diag-1", error.diagnosticId());
    }
}
