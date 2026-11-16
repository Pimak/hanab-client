package com.pimak.hanab.client.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pimak.hanab.client.model.*;
import com.pimak.hanab.client.protocol.*;
import com.pimak.hanab.client.game.GameState;
import com.pimak.hanab.client.game.GameStateReducer;
import com.pimak.hanab.client.replay.SharedReplayState;
import com.pimak.hanab.client.replay.SharedReplayStateReducer;
import com.pimak.hanab.client.replay.ReplayAction;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Default JDK-only implementation. Server responses remain authoritative and are exposed as events. */
public final class HanabClientImpl implements HanabClient, RawProtocolClient {
    private static final int MAX_MESSAGE_BYTES = 1_048_576;
    private final ClientConfig config; private final HttpClient http; private final java.net.CookieManager cookies;
    private final WireCodec codec = new WireCodec(); private final SubmissionPublisher<ServerEvent> events = new SubmissionPublisher<>();
    private final ConcurrentMap<Long, Table> tables = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, GameState> gameStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, GameState> finishedGameStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, SharedReplayState> sharedReplayStates = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, ConcurrentMap<String, TurnSuggestion>> turnSuggestions = new ConcurrentHashMap<>();
    private final SharedReplayStateReducer sharedReplayReducer = new SharedReplayStateReducer();
    private final Set<Long> startedTables = ConcurrentHashMap.newKeySet();
    private final Set<Long> gameInitRequested = ConcurrentHashMap.newKeySet();
    private final Set<Long> gameStateRequested = ConcurrentHashMap.newKeySet();
    private final Set<Long> gameStateLoaded = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, Set<String>> seenGameMessages = new ConcurrentHashMap<>();
    private final GameStateReducer gameStateReducer = new GameStateReducer();
    private final ClientMetrics metrics = new ClientMetrics();
    private final java.util.concurrent.atomic.AtomicLong connectionGeneration = new java.util.concurrent.atomic.AtomicLong();
    private final CopyOnWriteArrayList<CompletableFuture<LobbySnapshot>> pendingLobbies = new CopyOnWriteArrayList<>();
    private final Object pageQueueLock = new Object();
    private CompletionStage<Void> pageQueue = CompletableFuture.completedFuture(null);
    private final Object sendAndWaitQueueLock = new Object();
    private CompletionStage<Void> sendAndWaitTail = CompletableFuture.completedFuture(null);
    private final Set<CompletableFuture<?>> pendingWaits = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicLong nextRequestId = new java.util.concurrent.atomic.AtomicLong(1);
    private final ConcurrentMap<Long, PendingRequest> pendingRequests = new ConcurrentHashMap<>();
    private volatile boolean lobbyLoaded;
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Long, String> onlineUserNames = new ConcurrentHashMap<>();
    private volatile WebSocket socket; private volatile Session session; private volatile boolean closed; private volatile int reconnectAttempts;
    private volatile ScheduledFuture<?> heartbeat;
    private volatile long lastSocketActivity;
    private volatile ScheduledFuture<?> reconnectTask;
    private record PendingRequest(String command, CompletableFuture<WireMessage> waiter) {}
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor(r -> { var t = new Thread(r, "hanab-client-reconnect"); t.setDaemon(true); return t; });
    public HanabClientImpl(ClientConfig config) { this.config = Objects.requireNonNull(config); this.cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL); this.http = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(config.requestTimeout()).build(); }
    @Override public CompletionStage<Session> login(Credentials credentials, LoginOptions options) {
        Objects.requireNonNull(credentials); Objects.requireNonNull(options);
        if (closed) return CompletableFuture.failedFuture(new TransportException("client is closed"));
        if (session != null) return CompletableFuture.failedFuture(new AuthenticationException("client is already authenticated"));
        if (options == null) return CompletableFuture.failedFuture(new AuthenticationException("bot login options are required"));
        char[] secret = credentials.passwordCopy();
        String form;
        try {
            form = "username="+enc(credentials.username())+"&password="+enc(new String(secret))+"&createAccount="+options.createAccount()+"&client=bot";
        } finally { java.util.Arrays.fill(secret, '\0'); }
        return send("/login", "POST", form, "application/x-www-form-urlencoded").thenCompose(body -> { try { JsonNode n=codec.mapper().readTree(body); long rawId=n.path("userId").asLong(-1); if(rawId<1 || rawId>Integer.MAX_VALUE) return CompletableFuture.failedFuture(new AuthenticationException("login response did not contain a valid userId")); Ids.UserId id=new Ids.UserId(rawId); Session authenticated=new Session(id,new Ids.Username(credentials.username()),Instant.now(),"bot"); session=authenticated; return openSocket().thenApply(v -> authenticated).exceptionally(error -> { session=null; cookies.getCookieStore().removeAll(); throw new CompletionException(error); }); } catch(Exception e) { return failed(new AuthenticationException("invalid login response")); }}).exceptionallyCompose(error -> { Throwable cause=error instanceof CompletionException && error.getCause()!=null ? error.getCause() : error; if(cause instanceof TransportException transport && (transport.statusCode()==401 || transport.statusCode()==403)) return CompletableFuture.failedFuture(new AuthenticationException("authentication rejected")); return CompletableFuture.failedFuture(cause); });
    }
    @Override public CompletionStage<Session> identity() { if (session == null) return CompletableFuture.failedFuture(new AuthenticationException("identity requires an authenticated session")); return identitySnapshot().thenApply(snapshot -> { var current=session; return session=new Session(snapshot.userId().orElse(current.userId()), snapshot.username(), Instant.now(), "bot"); }); }
    @Override public CompletionStage<IdentitySnapshot> identitySnapshot() { if (session == null) return CompletableFuture.failedFuture(new AuthenticationException("identity requires an authenticated session")); return send("/api/v1/identity/token", "GET", "", null).thenApply(body -> { try { return IdentitySnapshot.from(codec.mapper().readTree(body)); } catch(Exception e) { throw new CompletionException(e); } }); }
    @Override public CompletionStage<Void> reconnect() { if (closed) return CompletableFuture.failedFuture(new TransportException("client is closed")); cancelReconnect(); closeSocket(); return openSocket(); }
    @Override public CompletionStage<Void> logout() { cancelReconnect(); return send("/logout","GET","",null).handle((ignored, error) -> { session=null; clearAllTableState(); cookies.getCookieStore().removeAll(); cancelReconnect(); closeSocket(); if (error != null) throw new CompletionException(error); return null; }); }
    @Override public CompletionStage<LobbySnapshot> loadLobby() { var result = new CompletableFuture<LobbySnapshot>(); pendingLobbies.add(result); if (lobbyLoaded) result.complete(snapshot()); command("lobbyLoad", codec.json(Map.of())).whenComplete((ignored, error) -> { if (error != null) { pendingLobbies.remove(result); result.completeExceptionally(error); } }); result.orTimeout(config.requestTimeout().toMillis(), TimeUnit.MILLISECONDS).whenComplete((ignored, error) -> pendingLobbies.remove(result)); return result; }
    @Override public Optional<GameState> gameState(Ids.TableId tableId) { return Optional.ofNullable(gameStates.get(Objects.requireNonNull(tableId).value())); }
    @Override public Optional<GameState> finishedGameState(Ids.TableId tableId) { return Optional.ofNullable(finishedGameStates.get(Objects.requireNonNull(tableId).value())); }
    @Override public Optional<SharedReplayState> sharedReplayState(Ids.TableId tableId) { return Optional.ofNullable(sharedReplayStates.get(Objects.requireNonNull(tableId).value())); }
    @Override public List<TurnSuggestion> turnSuggestions(Ids.TableId tableId) {
        Objects.requireNonNull(tableId, "tableId");
        var values = turnSuggestions.get(tableId.value());
        return values == null ? List.of() : List.copyOf(values.values());
    }
    @Override public CompletionStage<Void> loadGameInit(Ids.TableId tableId) { return tableCommand("gameInitLoad", tableId); }
    @Override public CompletionStage<Void> loadGameState(Ids.TableId tableId) { return tableCommand("gameStateLoad", tableId); }
    @Override public CompletionStage<Void> markGameLoaded(Ids.TableId tableId) {
        Objects.requireNonNull(tableId, "tableId");
        long id = tableId.value();
        if (!gameStateLoaded.add(id)) return CompletableFuture.completedFuture(null);
        return tableCommand("gameLoaded", tableId).whenComplete((ignored, error) -> { if (error != null) gameStateLoaded.remove(id); });
    }
    @Override public CompletionStage<Void> reattendTable(Ids.TableId tableId) { return tableCommand("tableReattend", tableId); }
    @Override public CompletionStage<Void> command(String command, JsonNode payload) { metrics.command(); WebSocket ws=socket; if(ws==null){metrics.error();return CompletableFuture.failedFuture(new TransportException("WebSocket is not connected"));} return sendRequest(ws, command, payload, null).whenComplete((x,e)->{if(e!=null)metrics.error();}); }
    @Override public CompletionStage<ServerEvent> sendAndWait(String command, JsonNode payload, java.util.function.Predicate<ServerEvent> completion, Duration timeout) {
        Objects.requireNonNull(completion, "completion"); Objects.requireNonNull(timeout, "timeout");
        final long timeoutMillis;
        try { timeoutMillis = timeout.toMillis(); }
        catch (ArithmeticException error) { return CompletableFuture.failedFuture(new IllegalArgumentException("timeout is too large", error)); }
        if (timeout.isNegative() || timeout.isZero() || timeoutMillis < 1) return CompletableFuture.failedFuture(new IllegalArgumentException("timeout must be at least 1ms"));
        var result = new CompletableFuture<ServerEvent>();
        pendingWaits.add(result);
        if (closed || socket == null) {
            pendingWaits.remove(result);
            result.completeExceptionally(new TransportException("WebSocket is not connected"));
            return result;
        }
        result.orTimeout(timeoutMillis, TimeUnit.MILLISECONDS);
        var release = new CompletableFuture<Void>();
        CompletionStage<Void> predecessor;
        synchronized (sendAndWaitQueueLock) { predecessor = sendAndWaitTail; sendAndWaitTail = release; }
        var replyRef = new java.util.concurrent.atomic.AtomicReference<CompletableFuture<WireMessage>>();
        result.whenComplete((ignored, error) -> { pendingWaits.remove(result); var reply = replyRef.get(); if (error != null && reply != null) reply.cancel(true); release.complete(null); });
        predecessor.whenComplete((ignored, error) -> {
            if (result.isDone() || closed || socket == null) { result.completeExceptionally(new TransportException("WebSocket is not connected")); return; }
            var reply = new CompletableFuture<WireMessage>();
            replyRef.set(reply);
            reply.whenComplete((message, replyError) -> {
                if (replyError != null) { result.completeExceptionally(unwrap(replyError)); return; }
                var event = new ServerEvent(Instant.now(), message);
                try { applyLobbyEvent(message); applyGameEvent(message); events.submit(event); if (completion.test(event)) result.complete(event); else result.completeExceptionally(new TransportException("unexpected reply for " + command)); }
                catch (Throwable protocolError) { result.completeExceptionally(protocolError); }
            });
            sendRequest(socket, command, payload, reply).whenComplete((sent, sendError) -> { if (sendError != null) result.completeExceptionally(unwrap(sendError)); });
        });
        return result;
    }
    private CompletionStage<Void> obj(String command, ObjectNode o) { return command(command,o); }
    private CompletionStage<Void> tableCommand(String command, Ids.TableId tableId) {
        return obj(command, codec.mapper().createObjectNode().put("tableId", tableId.value()));
    }
    @Override public CompletionStage<Void> createTable(JsonNode settings,String password){if(password!=null&&password.isEmpty())return CompletableFuture.failedFuture(new IllegalArgumentException("password must be absent or non-empty"));ObjectNode o=codec.mapper().createObjectNode();o.set("settings",Objects.requireNonNull(settings,"settings")); if(password!=null)o.put("password",password); return obj("tableCreate",o);}
    @Override public CompletionStage<Void> createTable(TableSettings settings, String password){return createTable(settings.toJson(), password);}
    @Override public CompletionStage<Void> joinTable(Ids.TableId id,String password){if(password!=null&&password.isEmpty())return CompletableFuture.failedFuture(new IllegalArgumentException("password must be absent or non-empty"));ObjectNode o=codec.mapper().createObjectNode().put("tableId",id.value()); if(password!=null)o.put("password",password); return obj("tableJoin",o);}
    @Override public CompletionStage<Void> leaveTable(Ids.TableId id){return obj("tableUnattend",codec.mapper().createObjectNode().put("tableId",id.value())).thenRun(() -> clearTableState(id.value()));}
    @Override public CompletionStage<Void> leaveLobbyTable(Ids.TableId id){return obj("tableLeave",codec.mapper().createObjectNode().put("tableId",id.value())).thenRun(() -> clearTableState(id.value()));}
    @Override public CompletionStage<Void> spectate(Ids.TableId id,Integer seat){ObjectNode o=codec.mapper().createObjectNode().put("tableId",id.value()); if(seat!=null)o.put("shadowingPlayerIndex",seat); return obj("tableSpectate",o);}
    @Override public CompletionStage<Void> startTable(Ids.TableId id,List<Ids.UserId> players){Objects.requireNonNull(players,"intendedPlayers"); if(players.isEmpty() || players.size()>6 || players.stream().anyMatch(Objects::isNull)) return CompletableFuture.failedFuture(new IllegalArgumentException("intendedPlayers must contain 1 to 6 user ids")); var ids=codec.mapper().createArrayNode(); players.forEach(player -> ids.add(player.value())); return obj("tableStart",codec.mapper().createObjectNode().put("tableId",id.value()).set("intendedPlayerIds",ids));}
    @Override public CompletionStage<Void> play(Ids.TableId id,Ids.CardOrder order){return action(id,"play",order.value());}
    @Override public CompletionStage<Void> discard(Ids.TableId id,Ids.CardOrder order){return action(id,"discard",order.value());}
    private CompletionStage<Void> action(Ids.TableId id,String type,int order){ObjectNode a=codec.mapper().createObjectNode().put("type",type).put("order",order); return obj("playerAction",codec.mapper().createObjectNode().put("tableId",id.value()).set("action",a));}
    @Override public CompletionStage<Void> clue(Ids.TableId id,int receiver,JsonNode clue){ObjectNode a=codec.mapper().createObjectNode().put("type","clue").put("receiverIndex",receiver).set("clue",clue); return obj("playerAction",codec.mapper().createObjectNode().put("tableId",id.value()).set("action",a));}
    @Override public CompletionStage<Void> clueAction(Ids.TableId id, int receiver, com.pimak.hanab.client.game.Clue clue) {
        Objects.requireNonNull(clue, "clue");
        return clue(id, receiver, clue.toJson(codec.mapper()));
    }
    @Override public CompletionStage<Void> pause(Ids.TableId id,boolean paused){return obj("pause",codec.mapper().createObjectNode().put("tableId",id.value()).put("paused",paused));}
    @Override public CompletionStage<Void> note(Ids.TableId id,Ids.CardOrder order,String text){return obj("note",codec.mapper().createObjectNode().put("tableId",id.value()).put("order",order.value()).put("text",text));}
    @Override public CompletionStage<Void> chat(String text,Ids.TableId id){ObjectNode o=codec.mapper().createObjectNode().put("text",text);if(id!=null)o.put("tableId",id.value());return obj("chat",o);}
    @Override public CompletionStage<Void> privateChat(String recipient,String text){return obj("chatPm",codec.mapper().createObjectNode().put("recipient",recipient).put("text",text));}
    @Override public CompletionStage<Void> privateChatHistory(String recipient,int limit){return privateChatHistoryResult(recipient, limit).thenApply(ignored -> (Void) null);}
    @Override public CompletionStage<List<com.pimak.hanab.client.social.ChatMessage>> privateChatHistoryResult(String recipient, int limit) {
        if (limit < 1 || limit > 100) return CompletableFuture.failedFuture(new IllegalArgumentException("private chat limit must be 1..100"));
        if (recipient != null && recipient.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("recipient must be absent or non-blank"));
        var payload = codec.mapper().createObjectNode().put("limit", limit);
        if (recipient != null) payload.put("recipient", recipient);
        return sendAndWait("chatPmHistory", payload,
                event -> event.message().command().equals("chatPmHistory") && event.chatList().isPresent(), config.requestTimeout())
                .thenApply(event -> event.chatList().orElseThrow());
    }
    @Override public CompletionStage<Void> typing(Ids.TableId id){return obj("chatTyping",codec.mapper().createObjectNode().put("tableId",id.value()));}
    @Override public CompletionStage<Void> createReplay(JsonNode source,JsonNode visibility){Objects.requireNonNull(source,"source"); Objects.requireNonNull(visibility,"visibility"); ObjectNode o=codec.mapper().createObjectNode();o.set("source",source);o.set("visibility",visibility);return obj("replayCreate",o);}
    @Override public CompletionStage<Void> createReplay(com.pimak.hanab.client.replay.ReplaySource source, com.pimak.hanab.client.replay.ReplayVisibility visibility, Integer shadowingPlayerIndex) {
        Objects.requireNonNull(source, "source"); Objects.requireNonNull(visibility, "visibility");
        ObjectNode o = codec.mapper().createObjectNode();
        o.set("source", source.toJson(codec.mapper()));
        o.set("visibility", visibility.toJson(codec.mapper()));
        if (shadowingPlayerIndex != null) { if (shadowingPlayerIndex < 0 || shadowingPlayerIndex > 5) return CompletableFuture.failedFuture(new IllegalArgumentException("shadowingPlayerIndex must be 0..5")); o.put("shadowingPlayerIndex", shadowingPlayerIndex); }
        return obj("replayCreate", o);
    }
    @Override public CompletionStage<ReplayDocument> loadReplay(Ids.GameId gameId) {
        Objects.requireNonNull(gameId, "gameId");
        return send("/export/" + gameId.value(), "GET", "", null).thenApply(body -> {
            try { return com.pimak.hanab.client.replay.ReplayDocumentCodec.importJson(body); }
            catch (com.pimak.hanab.client.protocol.ProtocolDecodeException error) { throw new CompletionException(error); }
            catch (Exception error) { throw new CompletionException(new TransportException("Invalid replay document", error)); }
        });
    }
    @Override public CompletionStage<Void> replayAction(Ids.TableId id,JsonNode action){return obj("replayAction",codec.mapper().createObjectNode().put("tableId",id.value()).set("action",action));}
    @Override public CompletionStage<Void> replayAction(Ids.TableId id, com.pimak.hanab.client.replay.ReplayAction action){Objects.requireNonNull(action,"action");return replayAction(id,action.raw());}
    @Override public CompletionStage<Void> suggestTurn(Ids.TableId id,int turn){if(turn<0 || turn>512)return CompletableFuture.failedFuture(new IllegalArgumentException("turn must be between 0 and 512"));return obj("tableSuggest",codec.mapper().createObjectNode().put("tableId",id.value()).put("turn",turn));}
    @Override public CompletionStage<Void> updateTable(Ids.TableId id,JsonNode settings){return obj("tableUpdate",codec.mapper().createObjectNode().put("tableId",id.value()).set("settings",settings));}
    @Override public CompletionStage<Void> updateTable(Ids.TableId id, TableSettings settings){return updateTable(id, settings.toJson());}
    @Override public CompletionStage<Void> setOwner(Ids.TableId id,Ids.UserId user){return obj("tableSetOwner",codec.mapper().createObjectNode().put("tableId",id.value()).put("userId",user.value()));}
    @Override public CompletionStage<Void> kick(Ids.TableId id,String username){return chat("/kick "+username,id);}
    @Override public CompletionStage<Void> restart(Ids.TableId id,boolean startNow){return obj("tableRestart",codec.mapper().createObjectNode().put("tableId",id.value()).put("startNow",startNow));}
    @Override public CompletionStage<Void> declineRestart(Ids.TableId id,boolean declined){return obj("tableRestartDecline",codec.mapper().createObjectNode().put("tableId",id.value()).put("declined",declined));}
    @Override public CompletionStage<Void> terminate(Ids.TableId id){return obj("tableTerminate",codec.mapper().createObjectNode().put("tableId",id.value()));}
    @Override public CompletionStage<Void> terminateVote(Ids.TableId id,boolean voted){return obj("tableTerminateVote",codec.mapper().createObjectNode().put("tableId",id.value()).put("voted",voted));}
    @Override public CompletionStage<Void> tag(Ids.TableId id,String tag,boolean add){if(tag==null || tag.isBlank() || tag.length()>100)return CompletableFuture.failedFuture(new IllegalArgumentException("tag must be 1 to 100 characters"));return obj(add?"tagAdd":"tagRemove",codec.mapper().createObjectNode().put("tableId",id.value()).put("tag",tag));}
    @Override public CompletionStage<Void> removeAllTags(Ids.TableId id){return obj("tagRemoveAll",codec.mapper().createObjectNode().put("tableId",id.value()));}
    @Override public CompletionStage<Void> relationship(String command,String username){if(command==null||command.isBlank()||username==null||username.isBlank())return CompletableFuture.failedFuture(new IllegalArgumentException("relationship command and username must be non-blank"));return obj(command,codec.mapper().createObjectNode().put("username",username));}
    @Override public CompletionStage<Void> relationship(com.pimak.hanab.client.social.RelationshipAction action) {
        Objects.requireNonNull(action, "action");
        return switch (action.type()) {
            case FRIEND -> addFriend(action.username());
            case UNFRIEND -> removeFriend(action.username());
            case LINK -> addLink(action.username());
            case UNLINK -> removeLink(action.username());
            case BLOCK -> blockUser(action.username());
            case UNBLOCK -> unblockUser(action.username());
            case KICK -> chat(action.rawText(), null);
        };
    }
    @Override public CompletionStage<Void> addFriend(String username){return relationship("friendAdd", username);}
    @Override public CompletionStage<Void> removeFriend(String username){return relationship("friendRemove", username);}
    @Override public CompletionStage<Void> addLink(String username){return relationship("linkAdd", username);}
    @Override public CompletionStage<Void> removeLink(String username){return relationship("linkRemove", username);}
    @Override public CompletionStage<Void> blockUser(String username){return relationship("blockAdd", username);}
    @Override public CompletionStage<Void> unblockUser(String username){return relationship("blockRemove", username);}
    @Override public CompletionStage<Void> listLinks(){return obj("linkList",codec.mapper().createObjectNode());}
    @Override public CompletionStage<Void> readChat(Ids.TableId id){return tableCommand("chatRead",id);}
    @Override public CompletionStage<Void> resetIdle(Ids.TableId id){return tableCommand("idleReset",id);}
    @Override public CompletionStage<Void> setInactive(boolean inactive){return obj("userInactive",codec.mapper().createObjectNode().put("inactive",inactive));}
    @Override public CompletionStage<Void> updateSettings(JsonNode settings,List<String> cleared){Objects.requireNonNull(settings,"settings"); Objects.requireNonNull(cleared,"cleared"); ObjectNode o=codec.mapper().createObjectNode(); o.set("settings",settings); o.set("cleared",codec.json(cleared)); return obj("settings",o);}
    @Override public CompletionStage<Void> loadHistory(int offset,int limit){if(offset<0 || limit<1 || limit>100)return CompletableFuture.failedFuture(new IllegalArgumentException("history page must have offset >= 0 and limit 1..100"));return obj("gameHistory",codec.mapper().createObjectNode().put("offset",offset).put("limit",limit));}
    @Override public CompletionStage<com.pimak.hanab.client.catalog.GamePage> historyPage(int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) return CompletableFuture.failedFuture(new IllegalArgumentException("history page must have offset >= 0 and limit 1..100"));
        var payload = codec.mapper().createObjectNode().put("offset", offset).put("limit", limit);
        return queuePage(() -> sendAndWait("gameHistory", payload, event -> event.gamePage().map(page -> page.offset() == offset && page.games().size() <= limit).orElse(false), config.requestTimeout())
                .thenApply(event -> event.gamePage().orElseThrow()));
    }
    @Override public CompletionStage<Void> searchTag(String tag){if(tag==null||tag.isBlank())return CompletableFuture.failedFuture(new IllegalArgumentException("tag must not be blank"));return obj("tagSearch",codec.mapper().createObjectNode().put("tag",tag));}
    @Override public CompletionStage<com.pimak.hanab.client.catalog.TagSearchPage> searchTagPage(String tag, int offset, int limit) { if(tag==null||tag.isBlank()||offset<0||limit<1||limit>100)return CompletableFuture.failedFuture(new IllegalArgumentException("invalid tag page")); return send("/api/v1/tag/"+encPath(tag),"GET","",null).thenApply(body -> { try { var full=com.pimak.hanab.client.catalog.TagSearchPage.from(codec.mapper().readTree(body)); if(offset>full.total()) throw new IllegalArgumentException("offset exceeds tag result"); int from=offset>Integer.MAX_VALUE?full.games().size():(int)offset; int to=Math.min(full.games().size(), from+limit); return new com.pimak.hanab.client.catalog.TagSearchPage(full.games().subList(from,to),offset,full.total()); } catch(Exception e) { throw new CompletionException(e); } }); }
    @Override public CompletionStage<Void> seedGames(String seed,int offset,int limit){try { var query = new com.pimak.hanab.client.catalog.SeedGamesQuery(seed, offset, limit); return obj("seedGames",codec.mapper().createObjectNode().put("seed",query.seed()).put("offset",query.offset()).put("limit",query.limit())); } catch (IllegalArgumentException e) { return CompletableFuture.failedFuture(e); }}
    @Override public CompletionStage<SeedGamesPage> seedGamesPage(String seed, int offset, int limit) {
        try {
            var query = new com.pimak.hanab.client.catalog.SeedGamesQuery(seed, offset, limit);
            var payload = codec.mapper().createObjectNode().put("seed", query.seed()).put("offset", query.offset()).put("limit", query.limit());
            return queuePage(() -> sendAndWait("seedGames", payload, event -> event.seedGames().map(page -> page.seed().equals(query.seed()) && page.page().offset() == query.offset() && page.page().games().size() <= query.limit()).orElse(false), config.requestTimeout())
                    .thenApply(event -> event.seedGames().orElseThrow()));
        } catch (IllegalArgumentException error) { return CompletableFuture.failedFuture(error); }
    }
    @Override public ClientMetrics.Snapshot metrics(){return metrics.snapshot();}
    @Override public Flow.Publisher<ServerEvent> events(){return events;}
    private <T> CompletionStage<T> queuePage(java.util.function.Supplier<? extends CompletionStage<T>> operation) {
        var result = new CompletableFuture<T>();
        synchronized (pageQueueLock) {
            CompletionStage<Void> previous = pageQueue;
            var current = new CompletableFuture<Void>();
            pageQueue = current;
            previous.whenComplete((ignored, previousError) -> {
                if (previousError != null) { current.complete(null); result.completeExceptionally(previousError); return; }
                try { operation.get().whenComplete((value, error) -> { current.complete(null); if (error == null) result.complete(value); else result.completeExceptionally(error); }); }
                catch (Throwable error) { current.complete(null); result.completeExceptionally(error); }
            });
        }
        return result;
    }
    private LobbySnapshot snapshot() { return new LobbySnapshot(new ArrayList<>(tables.values()), new ArrayList<>(onlineUsers)); }
    private void applyLobbyEvent(WireMessage message) {
        JsonNode payload = message.payload();
        validateLobbyEnvelope(message.command(), payload);
        switch (message.command()) {
            case "tableList" -> {
                var previousIds = new java.util.HashSet<Long>(tables.keySet());
                previousIds.addAll(startedTables); previousIds.addAll(gameStates.keySet());
                tables.clear(); JsonNode values = payload.path("tables"); values.forEach(this::rememberTable);
                previousIds.removeAll(tables.keySet()); previousIds.forEach(this::clearTableState);
                lobbyLoaded = true; var snapshot = snapshot(); for (var result : pendingLobbies) { result.complete(snapshot); pendingLobbies.remove(result); }
            }
            case "table" -> rememberTable(payload);
            case "tableRemoved", "tableBooted" -> { long id = payload.path("tableId").asLong(-1); tables.remove(id); clearTableState(id); }
            case "userList" -> { onlineUsers.clear(); onlineUserNames.clear(); JsonNode users = payload.path("users"); if (users.isArray()) users.forEach(user -> { long id=user.path("userId").asLong(-1); String name = user.isTextual() ? user.asText() : user.path("username").asText(null); if (name != null) { onlineUsers.add(name); if (id >= 0) onlineUserNames.put(id,name); } }); }
            case "user", "userAdded" -> { long id=payload.path("userId").asLong(-1); String name = payload.path("username").asText(null); if (name != null) { onlineUsers.add(name); if (id >= 0) onlineUserNames.put(id,name); } }
            case "userRemoved" -> { long id=payload.path("userId").asLong(-1); String name = onlineUserNames.remove(id); if (name != null) onlineUsers.remove(name); }
            default -> { }
        }
    }
    private static void validateLobbyEnvelope(String command, JsonNode payload) {
        switch (command) {
            case "tableList" -> { if (payload.get("tables") == null || !payload.get("tables").isArray()) throw new IllegalArgumentException("tableList.tables must be an array"); }
            case "table" -> requiredLobbyId(payload);
            case "tableRemoved", "tableBooted", "userRemoved" -> requiredLobbyId(payload);
            case "userList" -> { if (payload.get("users") == null || !payload.get("users").isArray()) throw new IllegalArgumentException("userList.users must be an array"); }
            case "user", "userAdded" -> { var id = payload.get("userId"); var username = payload.get("username"); if (!payload.isObject() || id == null || !id.isIntegralNumber() || !id.canConvertToLong() || id.longValue() < 1 || username == null || !username.isTextual() || username.textValue().isBlank()) throw new IllegalArgumentException("user event is invalid"); }
            default -> { }
        }
    }
    private static long requiredLobbyId(JsonNode payload) {
        var value = payload.get("tableId");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0 || value.longValue() > 9_007_199_254_740_991L) throw new IllegalArgumentException("lobby event is missing a valid id");
        return value.longValue();
    }
    private void applyGameEvent(WireMessage message) {
        String command = message.command();
        if (command.equals("turnSuggestion")) {
            long suggestionTable = requiredTableId(message.payload());
            var username = message.payload().get("username"); var turn = message.payload().get("turn");
            if (username == null || !username.isTextual() || username.textValue().isBlank() || turn == null || !turn.isIntegralNumber() || !turn.canConvertToInt() || turn.intValue() < 0 || turn.intValue() > 512)
                throw new IllegalArgumentException("turnSuggestion is invalid");
            var suggestion = new TurnSuggestion(new Ids.TableId(suggestionTable), username.textValue(), turn.intValue());
            turnSuggestions.computeIfAbsent(suggestionTable, ignored -> new ConcurrentHashMap<>()).put(suggestion.username(), suggestion);
            return;
        }
        if (command.equals("replayAction")) {
            long replayTable = requiredTableId(message.payload());
            var actionNode = message.payload().get("action");
            if (actionNode == null || !actionNode.isObject()) throw new IllegalArgumentException("replayAction is missing action");
            ReplayAction action = ReplayAction.from(actionNode);
            sharedReplayStates.compute(replayTable, (ignored, current) -> sharedReplayReducer.reduce(current == null ? new SharedReplayState() : current, action));
            return;
        }
        if (!command.equals("tableStarted") && !command.equals("gameFinished")
                && !command.equals("gameInit") && !command.equals("gameState") && !command.equals("gameEvent")
                && !Set.of("clock", "connected", "idleWarning", "cardIdentities", "note", "noteList", "pause", "votes", "spectators").contains(command)) return;
        long tableId = requiredTableId(message.payload());
        if (tableId >= 0 && command.equals("tableStarted")) {
            startedTables.add(tableId);
            sharedReplayStates.remove(tableId);
            turnSuggestions.remove(tableId);
            gameStates.remove(tableId);
            finishedGameStates.remove(tableId);
            gameInitRequested.remove(tableId);
            gameStateRequested.remove(tableId);
            gameStateLoaded.remove(tableId);
            seenGameMessages.put(tableId, ConcurrentHashMap.newKeySet());
            requestGameInit(tableId);
            return;
        }
        if (command.equals("gameFinished")) { var finalState = gameStates.get(tableId); if (finalState != null) finishedGameStates.put(tableId, finalState); var snapshot = message.payload().get("sharedReplay"); if (snapshot != null) sharedReplayStates.put(tableId, SharedReplayState.fromSnapshot(snapshot)); clearLiveTableState(tableId, true); return; }
        if (command.equals("cardIdentities")) {
            var deck = message.payload().get("deck");
            if (deck == null) throw new IllegalArgumentException("cardIdentities is missing deck");
            var live = gameStates.get(tableId); if (live != null) gameStates.put(tableId, live.withCardIdentities(deck));
            var finished = finishedGameStates.get(tableId); if (finished != null) finishedGameStates.put(tableId, finished.withCardIdentities(deck));
            return;
        }
        if (tableId < 0 || (!startedTables.contains(tableId) && !(command.equals("cardIdentities") && finishedGameStates.containsKey(tableId)))) return;
        if (command.equals("gameEvent")) {
            var seen = seenGameMessages.computeIfAbsent(tableId, ignored -> ConcurrentHashMap.newKeySet());
            if (!seen.add(message.payload().toString())) return;
        }
        if (Set.of("clock", "connected", "idleWarning", "cardIdentities", "note", "noteList", "pause", "votes", "spectators").contains(command)) {
            var current = gameStates.get(tableId);
            if (current != null) gameStates.put(tableId, current.withPatch(message.payload()));
            return;
        }
        synchronized (gameStates) {
            GameState previous = gameStates.getOrDefault(tableId, GameState.empty());
            boolean init = command.equals("gameInit");
            gameStates.put(tableId, init ? gameStateReducer.reduce(message.payload()) : gameStateReducer.reduce(previous, message.payload()));
        }
        if (command.equals("gameInit")) { if (!gameStateLoaded.contains(tableId)) requestGameState(tableId); }
        else if (command.equals("gameState")) {
            markGameLoaded(new Ids.TableId(tableId));
            gameStateRequested.remove(tableId);
        }
    }
    private static long requiredTableId(JsonNode payload) {
        JsonNode value = payload.get("tableId");
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0 || value.longValue() > 9_007_199_254_740_991L)
            throw new IllegalArgumentException("event is missing a valid tableId");
        return value.longValue();
    }
    private void requestGameInit(long tableId) {
        if (!gameInitRequested.add(tableId)) return;
        loadGameInit(new Ids.TableId(tableId)).whenComplete((ignored, error) -> {
            if (error != null) {
                gameInitRequested.remove(tableId);
                publishProtocolError("gameInitLoad failed for table " + tableId, error);
            }
        });
    }
    private void requestGameState(long tableId) {
        if (!gameStateRequested.add(tableId)) return;
        loadGameState(new Ids.TableId(tableId)).whenComplete((ignored, error) -> {
            if (error != null) {
                gameStateRequested.remove(tableId);
                publishProtocolError("gameStateLoad failed for table " + tableId, error);
            }
        });
    }
    private void publishProtocolError(String message, Throwable error) {
        events.submit(new ServerEvent(Instant.now(), new WireMessage("protocolError",
                codec.json(Map.of("message", message,
                        "cause", error.getMessage() == null ? "unknown" : error.getMessage())))));
    }
    private void rememberTable(JsonNode raw) { long id = raw.path("tableId").asLong(-1); if (id < 0) return; String phase = raw.path("phase").asText(raw.path("stage").asText("unknown")); String owner = raw.path("owner").isObject() ? raw.path("owner").path("username").asText(null) : raw.path("owner").asText(null); tables.put(id, new Table(new Ids.TableId(id), phase, owner, raw)); }
    private CompletionStage<Void> openSocket(){ long generation=connectionGeneration.incrementAndGet(); URI base=config.baseUri(); String scheme=base.getScheme().equals("https")?"wss":"ws"; String basePath=base.getPath()==null||base.getPath().isBlank()?"/":(base.getPath().endsWith("/")?base.getPath():base.getPath()+"/"); URI u=URI.create(scheme+"://"+base.getHost()+(base.getPort()<0?"":":"+base.getPort())+basePath+"ws?client=bot"); var builder=http.newWebSocketBuilder(); String cookie=cookieHeader(u); if(!cookie.isBlank()) builder.header("Cookie",cookie); return builder.buildAsync(u,new Listener(generation)).orTimeout(config.requestTimeout().toMillis(),TimeUnit.MILLISECONDS).thenCompose(ws->{if(generation!=connectionGeneration.get()) { ws.abort(); return CompletableFuture.failedFuture(new TransportException("stale WebSocket connection")); } socket=ws;startHeartbeat(ws);try { CompletableFuture<Void> chain=sendOnSocket(ws,"lobbyLoad",codec.json(Map.of())); for(long tableId:new ArrayList<>(startedTables)){ gameInitRequested.remove(tableId); gameStateRequested.remove(tableId); chain=chain.thenCompose(ignored -> sendOnSocket(ws,"tableReattend",codec.mapper().createObjectNode().put("tableId",tableId))); } return chain.orTimeout(config.requestTimeout().toMillis(),TimeUnit.MILLISECONDS).thenApply(ignored->{reconnectAttempts=0;return (Void)null;}).whenComplete((ignored,error)->{if(error!=null && socket==ws){socket=null;if(heartbeat!=null)heartbeat.cancel(false);ws.abort();}}); } catch (Exception e) { ws.abort(); return CompletableFuture.failedFuture(new TransportException("WebSocket bootstrap failed",e)); }}); }
    private CompletableFuture<Void> sendOnSocket(WebSocket ws,String command,JsonNode payload){ return sendRequest(ws, command, payload, null); }
    private CompletableFuture<Void> sendRequest(WebSocket ws, String command, JsonNode payload, CompletableFuture<WireMessage> waiter) {
        if (ws == null) return CompletableFuture.failedFuture(new TransportException("WebSocket is not connected"));
        long id = nextRequestId.getAndUpdate(value -> value >= 9_007_199_254_740_991L ? 1 : value + 1);
        pendingRequests.put(id, new PendingRequest(command, waiter));
        try {
            String frame = codec.encodeRequest(id, command, payload);
            return ws.sendText(frame, true).thenApply(ignored -> (Void) null).toCompletableFuture().whenComplete((ignored, error) -> {
                if (error != null) { var pending = pendingRequests.remove(id); if (pending != null && pending.waiter() != null) pending.waiter().completeExceptionally(unwrap(error)); }
            });
        } catch (Exception error) {
            pendingRequests.remove(id);
            if (waiter != null) waiter.completeExceptionally(error);
            return CompletableFuture.failedFuture(error);
        }
    }
    private void handleEventFrame(WireFrame frame) {
        WireMessage message = new WireMessage(frame.name(), frame.data(), WireMessage.Kind.EVENT, 0);
        applyLobbyEvent(message); applyGameEvent(message); metrics.event(); events.submit(new ServerEvent(Instant.now(), message));
    }
    private void handleReplyFrame(WireFrame frame) {
        PendingRequest pending = pendingRequests.remove(frame.id());
        if (pending == null) { metrics.error(); return; }
        JsonNode error = frame.error();
        if (error != null) {
            String message = error.isTextual() ? error.textValue() : error.path("message").asText("server rejected command");
            String code = error.isObject() && error.path("code").isTextual() ? error.path("code").textValue() : null;
            String diagnosticId = error.isObject() && error.path("diagnosticId").isTextual() ? error.path("diagnosticId").textValue() : null;
            var rejection = new ServerRejectionException(pending.command(), message, code, diagnosticId);
            if (pending.waiter() != null) pending.waiter().completeExceptionally(rejection);
            events.submit(new ServerEvent(Instant.now(), new WireMessage("error", error.isObject() ? error : codec.mapper().createObjectNode().put("message", message))));
            return;
        }
        JsonNode data = frame.data() == null ? codec.mapper().createObjectNode() : frame.data();
        WireMessage message = new WireMessage(pending.command(), data, WireMessage.Kind.REPLY, frame.id());
        if (pending.waiter() != null) pending.waiter().complete(message);
        else events.submit(new ServerEvent(Instant.now(), message));
    }
    private static Throwable unwrap(Throwable error) { return error instanceof CompletionException && error.getCause() != null ? error.getCause() : error; }
    private void startHeartbeat(WebSocket ws) { if (heartbeat != null) heartbeat.cancel(false); lastSocketActivity=System.nanoTime(); long interval = Math.max(1000L, config.requestTimeout().toMillis() / 3); heartbeat = reconnectScheduler.scheduleAtFixedRate(() -> { if (!closed && socket == ws && TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-lastSocketActivity) > config.requestTimeout().toMillis()) { ws.abort(); return; } ws.sendPing(ByteBuffer.allocate(0)).exceptionally(error -> { if (!closed && socket == ws) ws.abort(); return null; }); }, interval, interval, TimeUnit.MILLISECONDS); }
    private CompletionStage<String> send(String path,String method,String body,String type){try{URI target=config.baseUri().resolve(path.startsWith("/")?path.substring(1):path); HttpRequest.Builder b=HttpRequest.newBuilder(target).timeout(config.requestTimeout()); String cookie=cookieHeader(target); if(!cookie.isBlank())b.header("Cookie",cookie); if(type!=null)b.header("Content-Type",type); HttpRequest r=(method.equals("POST")?b.POST(HttpRequest.BodyPublishers.ofString(body,StandardCharsets.UTF_8)):b.GET()).build(); return http.sendAsync(r,limitedBodyHandler()).thenCompose(x->x.statusCode()>=200&&x.statusCode()<300?CompletableFuture.completedFuture(x.body()):CompletableFuture.failedFuture(new TransportException("HTTP "+x.statusCode(),x.statusCode()))).exceptionallyCompose(error->{Throwable cause=error instanceof CompletionException && error.getCause()!=null?error.getCause():error; return CompletableFuture.failedFuture(cause instanceof TransportException?cause:new TransportException("HTTP request failed",cause));});}catch(Exception e){return CompletableFuture.failedFuture(e instanceof TransportException?e:new TransportException("HTTP request failed",e));}}
    private static HttpResponse.BodyHandler<String> limitedBodyHandler(){return info->new LimitedBodySubscriber(MAX_MESSAGE_BYTES);}
    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<String>{private final int limit;private final CompletableFuture<String> result=new CompletableFuture<>();private final java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();private Flow.Subscription subscription;private int size;private LimitedBodySubscriber(int limit){this.limit=limit;}public CompletionStage<String> getBody(){return result;}public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}public void onNext(List<ByteBuffer> buffers){if(result.isDone())return;for(ByteBuffer buffer:buffers){if(buffer.remaining()>limit-size){subscription.cancel();result.completeExceptionally(new TransportException("HTTP response too large"));return;}byte[] copy=new byte[buffer.remaining()];buffer.get(copy);bytes.writeBytes(copy);size+=copy.length;}subscription.request(1);}public void onError(Throwable error){result.completeExceptionally(error);}public void onComplete(){if(!result.isDone())result.complete(new String(bytes.toByteArray(),StandardCharsets.UTF_8));}}
    private String cookieHeader(URI target){String host=target.getHost(); String path=target.getPath().isBlank()?"/":target.getPath(); boolean loopback="localhost".equalsIgnoreCase(host)||"127.0.0.1".equals(host)||"::1".equals(host); var scoped=cookies.getCookieStore().getCookies().stream().filter(cookie -> {String domain=cookie.getDomain(); String normalizedDomain=domain==null?null:(domain.startsWith(".")?domain.substring(1):domain); boolean domainMatches=normalizedDomain==null||normalizedDomain.isBlank()||host.equalsIgnoreCase(normalizedDomain)||host.endsWith("."+normalizedDomain); String cookiePath=cookie.getPath(); boolean pathMatches=cookiePath==null||cookiePath.isBlank()||path.startsWith(cookiePath); boolean secureAllowed=!cookie.getSecure() || "https".equalsIgnoreCase(target.getScheme()) || "wss".equalsIgnoreCase(target.getScheme()) || loopback; return domainMatches&&pathMatches&&secureAllowed;}).map(cookie -> cookie.getName()+"="+cookie.getValue()).collect(java.util.stream.Collectors.joining("; ")); if(!scoped.isBlank() || !loopback) return scoped; return cookies.getCookieStore().getCookies().stream().map(cookie -> cookie.getName()+"="+cookie.getValue()).collect(java.util.stream.Collectors.joining("; "));}
    private static String enc(String s){return URLEncoder.encode(s,StandardCharsets.UTF_8);}
    private static String encPath(String value){java.lang.StringBuilder b=new java.lang.StringBuilder();for(byte c:value.getBytes(StandardCharsets.UTF_8)){int n=c&0xff;if((n>='a'&&n<='z')||(n>='A'&&n<='Z')||(n>='0'&&n<='9')||"-._~".indexOf(n)>=0)b.append((char)n);else b.append('%').append(String.format("%02X",n));}return b.toString();}
    private static <T> CompletionStage<T> failed(Throwable e){return CompletableFuture.failedFuture(e instanceof CompletionException?e: new CompletionException(e));}
    private void failPendingWaits(Throwable error) { for (var pending : pendingWaits) pending.completeExceptionally(error); for (var entry : pendingRequests.entrySet()) { var request = pendingRequests.remove(entry.getKey()); if (request != null && request.waiter() != null) request.waiter().completeExceptionally(error); } }
    private void closeSocket(){connectionGeneration.incrementAndGet();failPendingWaits(new TransportException("WebSocket connection reset"));if(heartbeat!=null)heartbeat.cancel(false);if(socket!=null)socket.sendClose(WebSocket.NORMAL_CLOSURE,"closed");socket=null;}
    private void scheduleReconnect() { if (closed || session == null || (reconnectTask != null && !reconnectTask.isDone())) return; int attempt = ++reconnectAttempts; metrics.reconnect(); long delay = Math.min(config.reconnectMax().toMillis(), config.reconnectInitial().toMillis() * (1L << Math.min(attempt - 1, 10))); events.submit(new ServerEvent(Instant.now(), new WireMessage("reconnecting", codec.json(Map.of("attempt", attempt, "delayMs", delay))))); reconnectTask = reconnectScheduler.schedule(() -> { if (closed || session == null) { reconnectTask = null; return; } openSocket().whenComplete((ok, error) -> { reconnectTask=null; if (error != null && !closed && session != null) scheduleReconnect(); else if (error == null && !closed && session != null) events.submit(new ServerEvent(Instant.now(), new WireMessage("reconnected", codec.json(Map.of())))); }); }, delay, TimeUnit.MILLISECONDS); }
    private void cancelReconnect() { var task = reconnectTask; reconnectTask = null; if (task != null) task.cancel(false); }
    private void clearLiveTableState(long tableId) { clearLiveTableState(tableId, false); }
    private void clearLiveTableState(long tableId, boolean preserveSharedReplay) { if (tableId < 0) return; startedTables.remove(tableId); if (!preserveSharedReplay) sharedReplayStates.remove(tableId); turnSuggestions.remove(tableId); gameStates.remove(tableId); gameInitRequested.remove(tableId); gameStateRequested.remove(tableId); gameStateLoaded.remove(tableId); seenGameMessages.remove(tableId); }
    private void clearTableState(long tableId) { clearLiveTableState(tableId); finishedGameStates.remove(tableId); }
    private void clearAllTableState() { startedTables.clear(); sharedReplayStates.clear(); turnSuggestions.clear(); gameStates.clear(); finishedGameStates.clear(); gameInitRequested.clear(); gameStateRequested.clear(); gameStateLoaded.clear(); seenGameMessages.clear(); }
    @Override public void close(){closed=true;session=null;clearAllTableState();for(var pending:pendingLobbies) pending.completeExceptionally(new TransportException("client is closed"));pendingLobbies.clear();cookies.getCookieStore().removeAll();closeSocket();reconnectScheduler.shutdownNow();events.close();}
    private final class Listener implements WebSocket.Listener { private final long generation; private final StringBuilder text=new StringBuilder(); Listener(long generation){this.generation=generation;} public CompletionStage<?> onText(WebSocket ws,CharSequence data,boolean last){if(generation!=connectionGeneration.get() || socket!=ws) return null;lastSocketActivity=System.nanoTime();try{text.append(data);if(text.toString().getBytes(StandardCharsets.UTF_8).length>MAX_MESSAGE_BYTES) throw new IllegalStateException("server message too large");}catch(IllegalStateException error){text.setLength(0);metrics.error();events.submit(new ServerEvent(Instant.now(),new WireMessage("transportError",codec.json(Map.of("message",error.getMessage())))));ws.abort();return null;}if(last){String frame=text.toString();try{WireFrame decoded=codec.decodeFrame(frame);if(generation!=connectionGeneration.get() || socket!=ws) { text.setLength(0); return null; } if(decoded.kind()==WireFrame.Kind.EVENT) handleEventFrame(decoded); else handleReplyFrame(decoded); }catch(Exception error){metrics.error();events.submit(new ServerEvent(Instant.now(),new WireMessage("protocolError",codec.json(Map.of("message",error.getMessage()==null?"invalid server frame":error.getMessage())))));}text.setLength(0);}ws.request(1);return null;} public CompletionStage<?> onPong(WebSocket ws,ByteBuffer message){if(generation==connectionGeneration.get() && socket==ws) lastSocketActivity=System.nanoTime(); return null;} public void onError(WebSocket ws,Throwable e){if(generation!=connectionGeneration.get()) return;if(socket==ws)socket=null;if(heartbeat!=null)heartbeat.cancel(false);connectionGeneration.compareAndSet(generation,generation+1);failPendingWaits(new TransportException("WebSocket error", e));metrics.error();events.submit(new ServerEvent(Instant.now(),new WireMessage("transportError",codec.json(Map.of("message",e.getMessage()==null?"network error":e.getMessage())))));ws.abort();scheduleReconnect();} public CompletionStage<?> onClose(WebSocket ws,int status,String reason){if(generation!=connectionGeneration.get()) return CompletableFuture.completedFuture(null);if(socket==ws)socket=null;if(heartbeat!=null)heartbeat.cancel(false);connectionGeneration.compareAndSet(generation,generation+1);failPendingWaits(new TransportException("WebSocket closed: "+status+" "+reason));if(!closed){events.submit(new ServerEvent(Instant.now(),new WireMessage("transportError",codec.json(Map.of("message","WebSocket closed: "+status+" "+reason))))); scheduleReconnect();}return CompletableFuture.completedFuture(null);} }
    @Override public CompletionStage<com.pimak.hanab.client.catalog.VariantCatalog> loadVariantCatalog() {
        return com.pimak.hanab.client.catalog.VariantCatalog.load(path -> send(path, "GET", "", null).thenApply(body -> {
            try {
                JsonNode value = codec.mapper().readTree(body);
                if (value == null || !value.isObject()) throw new IllegalArgumentException("variant catalogue response must be an object");
                return codec.mapper().convertValue(value, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() { });
            } catch (Exception error) {
                throw new CompletionException(new TransportException("Invalid variant catalogue response", error));
            }
        }));
    }
    /** UTF-8 bounded accumulator used by the JDK WebSocket listener. */
    private static final class StringBuilder {
        private final java.lang.StringBuilder delegate = new java.lang.StringBuilder();
        private int bytes;
        StringBuilder append(CharSequence value) {
            Objects.requireNonNull(value, "value");
            int incoming = value.toString().getBytes(StandardCharsets.UTF_8).length;
            if (incoming > MAX_MESSAGE_BYTES - bytes) throw new IllegalStateException("server message too large");
            delegate.append(value);
            bytes += incoming;
            return this;
        }
        @Override public String toString() { return delegate.toString(); }
        void setLength(int length) {
            if (length != 0) throw new IllegalArgumentException("only reset is supported");
            delegate.setLength(0);
            bytes = 0;
        }
    }
}
