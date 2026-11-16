package com.pimak.hanab.client.social;

/** A typed social action addressed to another user. */
public sealed interface RelationshipAction
        permits FriendAction, BlockAction, KickAction, LinkAction, UnlinkAction, UnfriendAction, UnblockAction {
    String username();
    String rawText();

    default RelationshipActionType type() {
        if (this instanceof FriendAction) return RelationshipActionType.FRIEND;
        if (this instanceof BlockAction) return RelationshipActionType.BLOCK;
        if (this instanceof LinkAction) return RelationshipActionType.LINK;
        if (this instanceof UnlinkAction) return RelationshipActionType.UNLINK;
        if (this instanceof UnfriendAction) return RelationshipActionType.UNFRIEND;
        if (this instanceof UnblockAction) return RelationshipActionType.UNBLOCK;
        return RelationshipActionType.KICK;
    }
}
