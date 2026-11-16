package com.pimak.hanab.client.api;

import com.pimak.hanab.client.model.*;
import com.pimak.hanab.client.game.GameState;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.List;

public interface HanabClient extends AutoCloseable {
    CompletionStage<Session> login(Credentials credentials, LoginOptions options);
    CompletionStage<Session> identity();
    CompletionStage<IdentitySnapshot> identitySnapshot();
    CompletionStage<Void> reconnect();
    CompletionStage<Void> logout();
    CompletionStage<LobbySnapshot> loadLobby();
    Optional<GameState> gameState(Ids.TableId tableId);
    Optional<GameState> finishedGameState(Ids.TableId tableId);
    Optional<com.pimak.hanab.client.replay.SharedReplayState> sharedReplayState(Ids.TableId tableId);
    /** Current turn suggestions received for a shared replay table. */
    List<TurnSuggestion> turnSuggestions(Ids.TableId tableId);
    CompletionStage<Void> loadGameInit(Ids.TableId tableId);
    CompletionStage<Void> loadGameState(Ids.TableId tableId);
    CompletionStage<Void> markGameLoaded(Ids.TableId tableId);
    CompletionStage<Void> reattendTable(Ids.TableId tableId);
    CompletionStage<Void> createTable(TableSettings settings, String password);
    CompletionStage<Void> joinTable(Ids.TableId tableId, String password);
    CompletionStage<Void> leaveTable(Ids.TableId tableId);
    /** Leaves the lobby table; unlike leaveTable this uses the protocol's tableLeave command. */
    CompletionStage<Void> leaveLobbyTable(Ids.TableId tableId);
    CompletionStage<Void> spectate(Ids.TableId tableId, Integer shadowingPlayerIndex);
    CompletionStage<Void> startTable(Ids.TableId tableId, java.util.List<String> intendedPlayers);
    CompletionStage<Void> play(Ids.TableId tableId, Ids.CardOrder order);
    CompletionStage<Void> discard(Ids.TableId tableId, Ids.CardOrder order);
    CompletionStage<Void> clueAction(Ids.TableId tableId, int receiverIndex, com.pimak.hanab.client.game.Clue clue);
    CompletionStage<Void> pause(Ids.TableId tableId, String setting);
    CompletionStage<Void> note(Ids.TableId tableId, Ids.CardOrder order, String text);
    CompletionStage<Void> chat(String text, Ids.TableId tableId);
    CompletionStage<Void> privateChat(String recipient, String text);
    CompletionStage<Void> privateChatHistory(String recipient, int limit);
    CompletionStage<List<com.pimak.hanab.client.social.ChatMessage>> privateChatHistoryResult(String recipient, int limit);
    CompletionStage<Void> typing(Ids.TableId tableId);
    CompletionStage<Void> createReplay(com.pimak.hanab.client.replay.ReplaySource source,
                                       com.pimak.hanab.client.replay.ReplayVisibility visibility,
                                       Integer shadowingPlayerIndex);
    CompletionStage<ReplayDocument> loadReplay(Ids.GameId gameId);
    CompletionStage<Void> replayAction(Ids.TableId tableId, com.pimak.hanab.client.replay.ReplayAction action);
    CompletionStage<Void> suggestTurn(Ids.TableId tableId, int turn);
    CompletionStage<Void> updateTable(Ids.TableId tableId, TableSettings settings);
    CompletionStage<Void> setOwner(Ids.TableId tableId, Ids.UserId userId);
    CompletionStage<Void> kick(Ids.TableId tableId, String username);
    CompletionStage<Void> restart(Ids.TableId tableId, boolean hidePregame);
    CompletionStage<Void> declineRestart(Ids.TableId tableId, boolean declined);
    CompletionStage<Void> terminate(Ids.TableId tableId);
    CompletionStage<Void> terminateVote(Ids.TableId tableId);
    CompletionStage<Void> tag(Ids.TableId tableId, String tag, boolean add);
    CompletionStage<Void> removeAllTags(Ids.TableId tableId);
    CompletionStage<Void> relationship(String command, String username);
    CompletionStage<Void> relationship(com.pimak.hanab.client.social.RelationshipAction action);
    CompletionStage<Void> addFriend(String username);
    CompletionStage<Void> removeFriend(String username);
    CompletionStage<Void> addLink(String username);
    CompletionStage<Void> removeLink(String username);
    CompletionStage<Void> blockUser(String username);
    CompletionStage<Void> unblockUser(String username);
    CompletionStage<Void> listLinks();
    CompletionStage<Void> readChat(Ids.TableId tableId);
    CompletionStage<Void> resetIdle(Ids.TableId tableId);
    CompletionStage<Void> setInactive(boolean inactive);
    CompletionStage<Void> loadHistory(int offset, int limit);
    CompletionStage<com.pimak.hanab.client.catalog.VariantCatalog> loadVariantCatalog();
    CompletionStage<com.pimak.hanab.client.catalog.GamePage> historyPage(int offset, int limit);
    CompletionStage<Void> searchTag(String tag);
    CompletionStage<com.pimak.hanab.client.catalog.TagSearchPage> searchTagPage(String tag, int offset, int limit);
    CompletionStage<Void> seedGames(String seed, int offset, int limit);
    CompletionStage<SeedGamesPage> seedGamesPage(String seed, int offset, int limit);
    ClientMetrics.Snapshot metrics();
    Flow.Publisher<ServerEvent> events();
    @Override void close();
}
