package com.pimak.hanab.client.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Typed event union used by a shared-replay hypothetical. */
public sealed interface HypotheticalEvent permits HypotheticalEvent.Card, HypotheticalEvent.Clue,
        HypotheticalEvent.Turn, HypotheticalEvent.Morph, HypotheticalEvent.Unmorph {
    String type();
    JsonNode toJson();

    record Card(String type, int playerIndex, int order, Optional<JsonNode> identity) implements HypotheticalEvent {
        public Card { if (!Set.of("draw", "play", "discard", "misplay").contains(type)) throw new IllegalArgumentException("invalid hypothetical card type"); seat(playerIndex); checkOrder(order); identity = identity == null ? Optional.empty() : identity.map(JsonNode::deepCopy); }
        @Override public Optional<JsonNode> identity() { return identity.map(JsonNode::deepCopy); }
        @Override public JsonNode toJson() { var n=base(type).put("playerIndex",playerIndex).put("order",order); identity.ifPresent(v -> n.set("identity",v.deepCopy())); return n; }
    }
    record Clue(int playerIndex, int receiverIndex, List<Integer> orders, Optional<JsonNode> clue, Optional<Boolean> ignoreNegative) implements HypotheticalEvent {
        public Clue { seat(playerIndex); seat(receiverIndex); if (orders == null || orders.size() > 6 || orders.stream().anyMatch(v -> v == null || v < 0 || v > 65)) throw new IllegalArgumentException("invalid hypothetical clue orders"); orders=List.copyOf(orders); clue=clue==null?Optional.empty():clue.map(JsonNode::deepCopy); ignoreNegative=ignoreNegative==null?Optional.empty():ignoreNegative; }
        @Override public Optional<JsonNode> clue() { return clue.map(JsonNode::deepCopy); }
        @Override public String type() { return "clue"; }
        @Override public JsonNode toJson() { var n=base(type()).put("playerIndex",playerIndex).put("receiverIndex",receiverIndex); var a=n.putArray("orders"); orders.forEach(a::add); clue.ifPresent(v -> n.set("clue",v.deepCopy())); ignoreNegative.ifPresent(v -> n.put("ignoreNegative",v)); return n; }
    }
    record Turn(int turn, Optional<Integer> currentPlayerIndex) implements HypotheticalEvent {
        public Turn { if(turn<0||turn>512) throw new IllegalArgumentException("turn out of range"); currentPlayerIndex=currentPlayerIndex==null?Optional.empty():currentPlayerIndex; currentPlayerIndex.ifPresent(HypotheticalEvent::seat); }
        @Override public String type(){return "turn";}
        @Override public JsonNode toJson(){var n=base(type()).put("turn",turn); currentPlayerIndex.ifPresent(v->n.put("currentPlayerIndex",v));return n;}
    }
    record Morph(int order, Optional<Integer> suitIndex, Optional<Integer> rank) implements HypotheticalEvent {
        public Morph { checkOrder(order); suitIndex=suitIndex==null?Optional.empty():suitIndex; rank=rank==null?Optional.empty():rank; suitIndex.ifPresent(v->{if(v<0||v>5)throw new IllegalArgumentException("suitIndex out of range");}); rank.ifPresent(v->{if(v<1||v>5)throw new IllegalArgumentException("rank out of range");}); if(suitIndex.isEmpty()&&rank.isEmpty()) throw new IllegalArgumentException("morph identity must contain suit or rank"); }
        @Override public String type(){return "morph";}
        @Override public JsonNode toJson(){var id=new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode();suitIndex.ifPresent(v->id.put("suitIndex",v));rank.ifPresent(v->id.put("rank",v));return base(type()).put("order",order).set("identity",id);}
    }
    record Unmorph(int order) implements HypotheticalEvent { public Unmorph { checkOrder(order); } @Override public String type(){return "unmorph";} @Override public JsonNode toJson(){return base(type()).put("order",order);} }

    static HypotheticalEvent from(JsonNode value) {
        if(value==null||!value.isObject()||!value.path("type").isTextual())throw new IllegalArgumentException("invalid hypothetical event");
        return switch(value.path("type").textValue()) {
            case "draw","play","discard","misplay" -> { reject(value, Set.of("type","playerIndex","order","identity")); yield new Card(value.path("type").textValue(), integer(value,"playerIndex",0,5), integer(value,"order",0,65), optional(value.get("identity"))); }
            case "clue" -> { reject(value, Set.of("type","playerIndex","receiverIndex","orders","clue","ignoreNegative")); var orders=value.get("orders"); if(orders==null||!orders.isArray())throw new IllegalArgumentException("clue orders missing"); var list=new java.util.ArrayList<Integer>();orders.forEach(v->{if(!v.isIntegralNumber())throw new IllegalArgumentException("clue order must be an integer");list.add(v.intValue());}); var negative=value.get("ignoreNegative"); if(negative!=null&&!negative.isBoolean())throw new IllegalArgumentException("ignoreNegative must be boolean"); yield new Clue(integer(value,"playerIndex",0,5),integer(value,"receiverIndex",0,5),list,optional(value.get("clue")),negative==null?Optional.empty():Optional.of(negative.booleanValue())); }
            case "turn" -> { reject(value, Set.of("type","turn","currentPlayerIndex")); yield new Turn(integer(value,"turn",0,512),value.has("currentPlayerIndex")?Optional.of(integer(value,"currentPlayerIndex",0,5)):Optional.empty()); }
            case "morph" -> { reject(value, Set.of("type","order","identity")); var id=value.get("identity"); if(id==null||!id.isObject())throw new IllegalArgumentException("morph identity missing"); reject(id, Set.of("suitIndex","rank")); yield new Morph(integer(value,"order",0,65),id.has("suitIndex")?Optional.of(integer(id,"suitIndex",0,5)):Optional.empty(),id.has("rank")?Optional.of(integer(id,"rank",1,5)):Optional.empty()); }
            case "unmorph" -> { reject(value, Set.of("type","order")); yield new Unmorph(integer(value,"order",0,65)); }
            default -> throw new IllegalArgumentException("unknown hypothetical event");
        };
    }
    private static ObjectNode base(String type){return new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode().put("type",type);}
    private static Optional<JsonNode> optional(JsonNode n){return n==null||n.isNull()?Optional.empty():Optional.of(n.deepCopy());}
    private static int integer(JsonNode n,String f,int min,int max){var v=n.get(f);if(v==null||!v.isIntegralNumber()||!v.canConvertToInt()||v.intValue()<min||v.intValue()>max)throw new IllegalArgumentException(f+" out of range");return v.intValue();}
    private static void seat(int v){if(v<0||v>5)throw new IllegalArgumentException("seat out of range");}
    private static void checkOrder(int v){if(v<0||v>65)throw new IllegalArgumentException("order out of range");}
    private static void reject(JsonNode node, Set<String> known){var fields=node.fieldNames();while(fields.hasNext())if(!known.contains(fields.next()))throw new IllegalArgumentException("unknown hypothetical event field");}
}
