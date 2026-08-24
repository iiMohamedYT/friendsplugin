package com.friends.plugin.model;

public enum FriendStatus {
    ONLINE,
    INVISIBLE,
    DND; // Do Not Disturb

    public FriendStatus next() {
        FriendStatus[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public String display() {
        return switch (this) {
            case ONLINE -> "§aOnline";
            case INVISIBLE -> "§7Invisible";
            case DND -> "§cDo Not Disturb";
        };
    }
}
