package com.pimak.hanab.client.transport;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

/** JDK-only WebSocket lifecycle with a bounded exponential reconnect policy. */
public final class WebSocketTransport implements AutoCloseable {
    private static final int MAX_MESSAGE_BYTES = 1_048_576;
    private final URI uri; private final Duration timeout; private final Map<String,Object> handshake;
    private final HttpClient client; private final ScheduledExecutorService scheduler; private final Consumer<WebSocketEvent> events;
    private final int maxReconnects; private final Duration reconnectDelay;
    private volatile WebSocket socket; private volatile boolean closed; private volatile int reconnects;

    public WebSocketTransport(URI uri, Map<String,?> handshake, Consumer<WebSocketEvent> events) {
        this(uri, handshake, events, Duration.ofSeconds(10), 3, Duration.ofMillis(250));
    }
    public WebSocketTransport(URI uri, Map<String,?> handshake, Consumer<WebSocketEvent> events, Duration timeout, int maxReconnects, Duration reconnectDelay) {
        this.uri=Objects.requireNonNull(uri); if (uri.getUserInfo() != null) throw new IllegalArgumentException("WebSocket URI must not contain user information"); this.handshake=new LinkedHashMap<>(handshake == null ? Map.of() : handshake);
        this.events=events == null ? e -> {} : events; this.timeout=timeout; this.maxReconnects=Math.max(0,maxReconnects); this.reconnectDelay=reconnectDelay;
        this.client=HttpClient.newBuilder().connectTimeout(timeout).build(); this.scheduler=Executors.newSingleThreadScheduledExecutor(r -> { Thread t=new Thread(r,"hanab-websocket-reconnect"); t.setDaemon(true); return t; });
    }

    public CompletableFuture<Void> connect() { closed=false; reconnects=0; return connectInternal(false); }
    public CompletableFuture<Void> send(Map<String,?> message) { Objects.requireNonNull(message, "message"); Object command=message.get("command"); if (!(command instanceof String name) || name.isBlank()) return CompletableFuture.failedFuture(new TransportException("wire message requires a command")); Object payload=message.containsKey("payload") ? message.get("payload") : Map.of(); return send(name + " " + Json.stringify(payload)); }
    public CompletableFuture<Void> send(String json) { WebSocket ws=socket; if(ws==null) return CompletableFuture.failedFuture(new TransportException("WebSocket is not open")); if(json==null || json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_MESSAGE_BYTES) return CompletableFuture.failedFuture(new TransportException("WebSocket message too large")); return ws.sendText(json, true).thenApply(x -> null); }

    private CompletableFuture<Void> connectInternal(boolean retry) {
        if (closed) return CompletableFuture.failedFuture(new TransportException("WebSocket is closed"));
        CompletableFuture<Void> result = new CompletableFuture<>();
        client.newWebSocketBuilder().connectTimeout(timeout).buildAsync(withProtocolQuery(), new Listener(result, retry)).whenComplete((ws, error) -> { if(error != null) { handleFailure(error); result.completeExceptionally(new TransportException("WebSocket connection failed", error)); } });
        return result;
    }
    private URI withProtocolQuery() {
        if (handshake.isEmpty()) return uri;
        StringBuilder query = new StringBuilder(uri.getQuery() == null ? "" : uri.getQuery());
        for (var entry : handshake.entrySet()) {
            if (!(entry.getValue() instanceof String value) || value.isBlank()) continue;
            if (!entry.getKey().equals("client") && !entry.getKey().equals("version") && !entry.getKey().equals("account")) continue;
            if (query.length() > 0) query.append('&');
            query.append(java.net.URLEncoder.encode(entry.getKey(), java.nio.charset.StandardCharsets.UTF_8));
            query.append('=').append(java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8));
        }
        try { return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), query.toString(), uri.getFragment()); }
        catch (java.net.URISyntaxException e) { throw new IllegalArgumentException("invalid WebSocket URI", e); }
    }
    private void handleFailure(Throwable error) {
        emit(new WebSocketEvent(WebSocketEvent.Type.ERROR,null,-1,null,error));
        if (!closed && reconnects < maxReconnects) { int attempt=++reconnects; emit(new WebSocketEvent(WebSocketEvent.Type.RECONNECTING,null,attempt,null,null)); scheduler.schedule(() -> connectInternal(true), reconnectDelay.toMillis() * (1L << Math.min(10, attempt-1)), TimeUnit.MILLISECONDS); }
    }
    private void emit(WebSocketEvent e) { try { events.accept(e); } catch (RuntimeException ignored) { } }
    @Override public void close() { closed=true; scheduler.shutdownNow(); WebSocket ws=socket; socket=null; if(ws!=null) ws.sendClose(WebSocket.NORMAL_CLOSURE,"closed"); emit(new WebSocketEvent(WebSocketEvent.Type.CLOSED,null,WebSocket.NORMAL_CLOSURE,"closed",null)); }

    private final class Listener implements WebSocket.Listener {
        private final CompletableFuture<Void> connected; private final boolean retry; private final StringBuilder fragments=new StringBuilder();
        Listener(CompletableFuture<Void> connected, boolean retry) { this.connected=connected; this.retry=retry; }
        @Override public void onOpen(WebSocket ws) { socket=ws; reconnects=0; ws.request(1); emit(WebSocketEvent.of(WebSocketEvent.Type.OPEN)); emit(WebSocketEvent.of(WebSocketEvent.Type.HANDSHAKE_SENT)); if(retry) emit(WebSocketEvent.of(WebSocketEvent.Type.RECONNECTED)); connected.complete(null); }
        private int bytes;
        @Override public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last) { int incoming=data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length; if (incoming > MAX_MESSAGE_BYTES - bytes) { fragments.setLength(0); bytes=0; ws.abort(); emit(new WebSocketEvent(WebSocketEvent.Type.ERROR,null,-1,"message too large",null)); return null; } fragments.append(data); bytes += incoming; if(last){String text=fragments.toString();fragments.setLength(0);bytes=0;emit(new WebSocketEvent(WebSocketEvent.Type.MESSAGE,text,-1,null,null));} ws.request(1); return null; }
        @Override public CompletionStage<?> onClose(WebSocket ws,int code,String reason) { if(socket==ws)socket=null; emit(new WebSocketEvent(WebSocketEvent.Type.CLOSED,null,code,reason,null)); if(!closed)handleFailure(new TransportException("WebSocket closed: "+code)); return null; }
        @Override public void onError(WebSocket ws,Throwable error) { if(!connected.isDone()) { emit(new WebSocketEvent(WebSocketEvent.Type.ERROR,null,-1,null,error)); connected.completeExceptionally(new TransportException("WebSocket error",error)); } else if(!closed) handleFailure(error); }
    }
}
