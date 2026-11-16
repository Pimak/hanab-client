package com.pimak.hanab.client.social;

/** Commands understood by the Hanab chat command parser. */
public enum ChatCommand {
    PAUSE("pause"), REPLAY("replay"), TAG("tag"), FRIEND("friend"), BLOCK("block"),
    KICK("kick"), SET_VARIANT("setvariant"), SPLIT("split"), PM("pm"), PM_HISTORY("pmhistory"),
    LINK("link"), UNLINK("unlink"), UNFRIEND("unfriend"), UNBLOCK("unblock"), SET_LEADER("setleader"), TERMINATE("terminate"), RESTART("restart");

    private final String wireName;
    ChatCommand(String wireName) { this.wireName = wireName; }
    public String wireName() { return wireName; }

    static ChatCommand from(String value) {
        for (var command : values()) if (command.wireName.equalsIgnoreCase(value)) return command;
        if (value.equalsIgnoreCase("w") || value.equalsIgnoreCase("whisper") || value.equalsIgnoreCase("msg") || value.equalsIgnoreCase("tell")) return PM;
        return null;
    }
}
