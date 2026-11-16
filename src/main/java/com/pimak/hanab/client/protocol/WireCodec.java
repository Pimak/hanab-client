package com.pimak.hanab.client.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

public final class WireCodec {
    private final ObjectMapper mapper;
    public WireCodec(ObjectMapper mapper) { this.mapper = mapper; }
    public WireCodec() { this(new ObjectMapper()); }
    public WireMessage decode(String frame) throws IOException {
        if (frame == null || frame.isBlank()) throw new IOException("empty wire frame");
        int separator = frame.indexOf(' ');
        String command = separator < 0 ? frame : frame.substring(0, separator);
        if (command.isBlank() || command.chars().anyMatch(Character::isWhitespace)) throw new IOException("invalid wire command");
        String json = separator < 0 ? "{}" : frame.substring(separator + 1).trim();
        JsonNode payload = mapper.readTree(json);
        if (payload == null || !payload.isObject()) throw new IOException("wire payload must be an object");
        return new WireMessage(command, payload);
    }
    public String encode(WireMessage message) throws IOException { return message.command() + " " + mapper.writeValueAsString(message.payload()); }
    public JsonNode json(Object value) { return mapper.valueToTree(value); }
    public ObjectMapper mapper() { return mapper; }
}
