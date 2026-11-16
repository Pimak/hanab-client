package com.pimak.hanab.client.social;

import static org.junit.jupiter.api.Assertions.*;

import com.pimak.hanab.client.model.User;
import java.util.List;
import org.junit.jupiter.api.Test;

class SocialTest {
    @Test
    void parsesEveryHanabCommandAndPreservesRawText() {
        var inputs = List.of("/pause", "/replay game-1", "/tag \"great table\"", "/friend Alice",
                "/block Bob", "/kick Carol", "/setvariant black-hole", "/split 2");
        for (var input : inputs) {
            var parsed = assertInstanceOf(ParsedChatCommand.class, ChatCommandParser.parse(input));
            assertEquals(input, parsed.rawText());
            assertFalse(parsed.arguments().contains(input));
        }
        assertEquals(List.of("great table"), ((ParsedChatCommand) ChatCommandParser.parse(inputs.get(2))).arguments());
    }

    @Test
    void ordinaryAndUnknownLinesRemainDistinctAndLossless() {
        assertEquals("  hello  ", ((ChatText) ChatCommandParser.parse("  hello  ")).rawText());
        var unknown = assertInstanceOf(UnknownChatCommand.class, ChatCommandParser.parse("/future x"));
        assertEquals("/future x", unknown.rawText());
    }

    @Test
    void socialValuesAreTypedAndImmutable() {
        var user = new User(3, "Alice");
        var message = new SocialMessage(SocialContext.table(7), user, "  hi  ");
        assertEquals("  hi  ", message.rawText());
        assertEquals(new FriendAction("Bob"), new FriendAction("Bob", "/friend Bob"));
        var typing = new TypingNotification(SocialContext.privateConversation("Bob"), user, true);
        assertTrue(typing.typing());
        assertEquals(TypingState.TYPING, typing.state());
        assertEquals(RelationshipActionType.FRIEND,
                ChatCommandParser.parseRelationshipAction(" /friend Bob ").orElseThrow().type());
        assertThrows(UnsupportedOperationException.class,
                () -> new ParsedChatCommand(ChatCommand.TAG, List.of("x"), "/tag x").arguments().add("y"));
    }
}
