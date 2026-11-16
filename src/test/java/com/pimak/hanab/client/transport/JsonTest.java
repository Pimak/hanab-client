package com.pimak.hanab.client.transport;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test void roundTripsNestedValuesAndEscapes() {
        Map<String,Object> value = new LinkedHashMap<>(); value.put("text", "a\n\"b"); value.put("items", Arrays.asList(true, 2, null));
        var roundTrip = (Map<?, ?>) Json.parse(Json.stringify(value));
        assertEquals(value.get("text"), roundTrip.get("text"));
        assertEquals(Arrays.asList(true, 2, null).toString(), roundTrip.get("items").toString());
    }
    @Test void rejectsTrailingContent() { assertThrows(TransportException.class, () -> Json.parse("{} nope")); }
}
