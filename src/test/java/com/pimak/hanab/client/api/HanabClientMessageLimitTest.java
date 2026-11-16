package com.pimak.hanab.client.api;

import org.junit.jupiter.api.Test;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;

class HanabClientMessageLimitTest {
    @Test void principalWebSocketAccumulatorRejectsOversizedUtf8Frames() throws Exception {
        var type = Class.forName("com.pimak.hanab.client.api.HanabClientImpl$StringBuilder");
        var constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        var accumulator = constructor.newInstance();
        var append = type.getDeclaredMethod("append", CharSequence.class);
        append.setAccessible(true);
        var error = assertThrows(InvocationTargetException.class, () -> append.invoke(accumulator, "é".repeat(600_000)));
        assertInstanceOf(IllegalStateException.class, error.getCause());
    }
}
