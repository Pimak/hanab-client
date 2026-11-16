package com.pimak.hanab.client.protocol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class Json {
    private Json() {}

    static Object parse(String input) throws ProtocolDecodeException {
        try {
            var parser = new Parser(input);
            Object value = parser.value();
            parser.ws();
            if (!parser.done()) throw parser.error("trailing characters");
            return value;
        } catch (ProtocolDecodeException e) { throw e; }
        catch (RuntimeException e) { throw new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_JSON, "invalid JSON", e); }
    }

    static String write(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return '"' + escape(s) + '"';
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        if (value instanceof Map<?, ?> map) {
            var out = new StringBuilder("{"); boolean first = true;
            for (var entry : map.entrySet()) { if (!first) out.append(','); first = false;
                out.append(write(String.valueOf(entry.getKey()))).append(':').append(write(entry.getValue())); }
            return out.append('}').toString();
        }
        if (value instanceof Iterable<?> values) {
            var out = new StringBuilder("["); boolean first = true;
            for (var item : values) { if (!first) out.append(','); first = false; out.append(write(item)); }
            return out.append(']').toString();
        }
        throw new IllegalArgumentException("unsupported JSON value: " + value.getClass());
    }

    private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\b", "\\b").replace("\f", "\\f").replace("\n", "\\n")
            .replace("\r", "\\r").replace("\t", "\\t"); }

    private static final class Parser {
        final String s; int p;
        Parser(String s) { this.s = s; }
        boolean done() { return p == s.length(); }
        void ws() { while (!done() && Character.isWhitespace(s.charAt(p))) p++; }
        ProtocolDecodeException error(String message) { return new ProtocolDecodeException(ProtocolDecodeException.Code.INVALID_JSON, message + " at " + p); }
        Object value() throws ProtocolDecodeException { ws(); if (done()) throw error("expected value");
            return switch (s.charAt(p)) { case '{' -> object(); case '[' -> array(); case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE); case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null); default -> number(); }; }
        Object literal(String text, Object value) throws ProtocolDecodeException { if (!s.startsWith(text, p)) throw error("invalid literal"); p += text.length(); return value; }
        Map<String,Object> object() throws ProtocolDecodeException { p++; ws(); var map = new LinkedHashMap<String,Object>();
            if (!done() && s.charAt(p) == '}') { p++; return map; }
            while (true) { ws(); if (done() || s.charAt(p) != '"') throw error("expected object key"); String key = string(); ws();
                if (done() || s.charAt(p++) != ':') throw error("expected colon"); map.put(key, value()); ws();
                if (done()) throw error("unterminated object"); char c = s.charAt(p++); if (c == '}') return map; if (c != ',') throw error("expected comma"); }
        }
        List<Object> array() throws ProtocolDecodeException { p++; ws(); var list = new ArrayList<Object>();
            if (!done() && s.charAt(p) == ']') { p++; return list; }
            while (true) { list.add(value()); ws(); if (done()) throw error("unterminated array"); char c = s.charAt(p++); if (c == ']') return list; if (c != ',') throw error("expected comma"); }
        }
        String string() throws ProtocolDecodeException { if (s.charAt(p++) != '"') throw error("expected string"); var out = new StringBuilder();
            while (!done()) { char c = s.charAt(p++); if (c == '"') return out.toString(); if (c == '\\') { if (done()) throw error("bad escape"); c = s.charAt(p++); if (c == 'u') { if (p + 4 > s.length()) throw error("incomplete unicode escape"); String hex = s.substring(p, p + 4); try { out.append((char) Integer.parseInt(hex, 16)); } catch (NumberFormatException e) { throw error("invalid unicode escape"); } p += 4; } else out.append(switch (c) { case '"' -> '"'; case '\\' -> '\\'; case '/' -> '/'; case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t'; default -> throw error("unsupported escape"); }); } else out.append(c); }
            throw error("unterminated string"); }
        Number number() throws ProtocolDecodeException { int start = p; while (!done() && "-+0123456789.eE".indexOf(s.charAt(p)) >= 0) p++; String n = s.substring(start,p);
            try {
                if (n.indexOf('.') >= 0 || n.indexOf('e') >= 0 || n.indexOf('E') >= 0) return Double.valueOf(n);
                long value = Long.parseLong(n);
                return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? Integer.valueOf((int) value) : Long.valueOf(value);
            } catch (NumberFormatException e) { throw error("invalid number"); } }
    }
}
