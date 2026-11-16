package com.pimak.hanab.client.transport;

import java.util.*;

/** Small dependency-free JSON codec for transport payloads. */
final class Json {
    private Json() {}

    static Object parse(String text) {
        if (text == null) throw new TransportException("JSON payload is null");
        Parser p = new Parser(text);
        Object value = p.value();
        p.ws();
        if (!p.end()) throw new TransportException("Invalid JSON: trailing content");
        return value;
    }

    static String stringify(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return quote(s);
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> m) {
            StringJoiner j = new StringJoiner(",", "{", "}");
            for (var e : m.entrySet()) j.add(quote(String.valueOf(e.getKey())) + ":" + stringify(e.getValue()));
            return j.toString();
        }
        if (value instanceof Iterable<?> i) {
            StringJoiner j = new StringJoiner(",", "[", "]");
            for (Object item : i) j.add(stringify(item));
            return j.toString();
        }
        throw new IllegalArgumentException("Unsupported JSON value: " + value.getClass());
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) { case '"' -> b.append("\\\""); case '\\' -> b.append("\\\\"); case '\b' -> b.append("\\b"); case '\f' -> b.append("\\f"); case '\n' -> b.append("\\n"); case '\r' -> b.append("\\r"); case '\t' -> b.append("\\t"); default -> { if (c < 32) b.append(String.format("\\u%04x", (int)c)); else b.append(c); } }
        }
        return b.append('"').toString();
    }

    private static final class Parser {
        final String s; int i;
        Parser(String s) { this.s = s; }
        boolean end() { return i == s.length(); }
        void ws() { while (!end() && Character.isWhitespace(s.charAt(i))) i++; }
        Object value() {
            ws(); if (end()) throw bad();
            return switch (s.charAt(i)) { case '{' -> object(); case '[' -> array(); case '"' -> string(); case 't' -> literal("true", Boolean.TRUE); case 'f' -> literal("false", Boolean.FALSE); case 'n' -> literal("null", null); default -> number(); };
        }
        Object literal(String token, Object result) { if (!s.startsWith(token, i)) throw bad(); i += token.length(); return result; }
        Map<String,Object> object() { Map<String,Object> m = new LinkedHashMap<>(); i++; ws(); if (!end() && s.charAt(i)=='}') { i++; return m; } while (true) { ws(); if (end() || s.charAt(i)!='"') throw bad(); String k=string(); ws(); if (end() || s.charAt(i++)!=':') throw bad(); m.put(k,value()); ws(); if (end()) throw bad(); char c=s.charAt(i++); if (c=='}') return m; if (c!=',') throw bad(); }
        }
        List<Object> array() { List<Object> a=new ArrayList<>(); i++; ws(); if (!end()&&s.charAt(i)==']'){i++;return a;} while(true){a.add(value());ws();if(end())throw bad();char c=s.charAt(i++);if(c==']')return a;if(c!=',')throw bad();} }
        String string() { StringBuilder b=new StringBuilder(); i++; while(!end()){char c=s.charAt(i++);if(c=='"')return b.toString();if(c=='\\'){if(end())throw bad();char e=s.charAt(i++);b.append(switch(e){case '"'->'"';case '\\'->'\\';case '/'->'/';case 'b'->'\b';case 'f'->'\f';case 'n'->'\n';case 'r'->'\r';case 't'->'\t';case 'u'->{if(i+4>s.length())throw bad();int n=Integer.parseInt(s.substring(i,i+4),16);i+=4;yield(char)n;}default->throw bad();});}else b.append(c);}throw bad(); }
        Number number() { int start=i; if(s.charAt(i)=='-')i++; while(!end()&&Character.isDigit(s.charAt(i)))i++; if(!end()&&s.charAt(i)=='.'){i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;} if(!end()&&(s.charAt(i)=='e'||s.charAt(i)=='E')){i++;if(!end()&&(s.charAt(i)=='+'||s.charAt(i)=='-'))i++;while(!end()&&Character.isDigit(s.charAt(i)))i++;} String n=s.substring(start,i);try{if(!n.matches("[-+]?\\d+"))return Double.valueOf(n);long value=Long.parseLong(n);return value>=Integer.MIN_VALUE&&value<=Integer.MAX_VALUE?Integer.valueOf((int)value):Long.valueOf(value);}catch(NumberFormatException e){throw bad();} }
        TransportException bad(){return new TransportException("Invalid JSON at offset "+i);}
    }
}
