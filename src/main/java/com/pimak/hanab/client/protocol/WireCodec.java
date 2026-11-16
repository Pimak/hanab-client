package com.pimak.hanab.client.protocol;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

/** Codec for the current structured Hanab bot WebSocket protocol. */
public final class WireCodec {
    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;
    private final ObjectMapper mapper;

    public WireCodec(ObjectMapper mapper) { this.mapper = mapper; }
    public WireCodec() { this(new ObjectMapper()); }

    public String encodeRequest(long id, String request, JsonNode data) throws IOException {
        if (id < 1 || id > MAX_SAFE_INTEGER) throw new IOException("request id is out of range");
        if (request == null || !request.matches("[A-Za-z][A-Za-z0-9]*")) throw new IOException("invalid request name");
        if (data == null || !data.isObject()) throw new IOException("request data must be an object");
        ObjectNode frame = mapper.createObjectNode().put("id", id).put("request", request);
        frame.set("data", data);
        return mapper.writeValueAsString(frame);
    }

    public WireFrame decodeFrame(String frame) throws IOException {
        if (frame == null || frame.isBlank()) throw new IOException("empty wire frame");
        JsonNode value = mapper.readTree(frame);
        if (value == null || !value.isObject()) throw new IOException("wire frame must be an object");
        JsonNode event = value.get("event");
        if (event != null) {
            if (!event.isTextual() || event.textValue().isBlank()) throw new IOException("invalid event name");
            JsonNode data = value.get("data");
            if (data == null || !data.isObject()) throw new IOException("event data must be an object");
            return WireFrame.event(event.textValue(), data);
        }
        JsonNode reply = value.get("reply");
        if (reply != null) {
            if (!reply.isIntegralNumber() || !reply.canConvertToLong()) throw new IOException("invalid reply id");
            long id = reply.longValue();
            if (id < 1 || id > MAX_SAFE_INTEGER) throw new IOException("reply id is out of range");
            JsonNode data = value.get("data");
            if (data != null && !data.isObject()) throw new IOException("reply data must be an object");
            JsonNode error = value.get("error");
            if (error != null && !error.isObject() && !error.isTextual()) throw new IOException("invalid reply error");
            return WireFrame.reply(id, "reply", data, error);
        }
        throw new IOException("unknown structured wire frame");
    }

    public JsonNode json(Object value) { return mapper.valueToTree(value); }
    public ObjectMapper mapper() { return mapper; }
}
