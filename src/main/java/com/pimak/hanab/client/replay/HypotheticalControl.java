package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

/** Typed control union for a shared-replay hypothetical. */
public sealed interface HypotheticalControl permits HypotheticalControl.Start, HypotheticalControl.End,
        HypotheticalControl.Back, HypotheticalControl.ShowDrawnCards, HypotheticalControl.Event {
    JsonNode toJson();
    record Start() implements HypotheticalControl { public JsonNode toJson(){return node("start");} }
    record End() implements HypotheticalControl { public JsonNode toJson(){return node("end");} }
    record Back() implements HypotheticalControl { public JsonNode toJson(){return node("back");} }
    record ShowDrawnCards(boolean show) implements HypotheticalControl { public JsonNode toJson(){return node("showDrawnCards").put("show",show);} }
    record Event(HypotheticalEvent event) implements HypotheticalControl { public Event { Objects.requireNonNull(event); } public JsonNode toJson(){return node("event").set("event",event.toJson());} }
    private static com.fasterxml.jackson.databind.node.ObjectNode node(String type){return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("type",type);}
}
