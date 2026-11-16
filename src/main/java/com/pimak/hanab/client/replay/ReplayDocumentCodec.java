package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pimak.hanab.client.model.ReplayDocument;
import com.pimak.hanab.client.protocol.ProtocolCodec;
import com.pimak.hanab.client.protocol.ProtocolDecodeException;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Import/export of the complete replay document, including optional metadata. */
public final class ReplayDocumentCodec {
    private static final ObjectMapper JSON = new ObjectMapper();
    private ReplayDocumentCodec() { }

    public static ReplayDocument importJson(String json) throws ProtocolDecodeException {
        return ProtocolCodec.decodeReplayDocument(Objects.requireNonNull(json, "json"));
    }

    public static String exportJson(ReplayDocument document) {
        Objects.requireNonNull(document, "document");
        var root = new LinkedHashMap<String, Object>();
        document.seed().ifPresent(seed -> root.put("seed", seed));
        if (!document.playerNames().isEmpty()) root.put("playerNames", document.playerNames());
        if (!document.deck().isEmpty()) root.put("deck", document.deck());
        if (!document.actions().isEmpty()) root.put("actions", document.actions());
        if (!document.options().isEmpty()) root.put("options", document.options());
        if (!document.notes().isEmpty()) root.put("notes", document.notes());
        if (!document.characters().isEmpty()) root.put("characters", document.characters());
        document.unknownFields().forEach(root::putIfAbsent);
        try { return JSON.writeValueAsString(root); }
        catch (JsonProcessingException e) { throw new IllegalStateException("unable to encode replay document", e); }
    }
}
