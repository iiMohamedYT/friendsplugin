package com.friends.plugin.model;

import java.util.UUID;

public class PlayerSettings {

    private final UUID uuid;
    private FriendStatus status;
    private boolean friendRequestsEnabled;
    private boolean friendNotificationsEnabled; // join/leave notifications
    private boolean friendMessageNotificationsEnabled; // sound on message
    private boolean friendMessagesEnabled; // allow friends to message this player

    public PlayerSettings(UUID uuid) {
        this(uuid, FriendStatus.ONLINE, true, true, true, true);
    }

    public PlayerSettings(UUID uuid, FriendStatus status, boolean friendRequestsEnabled,
                           boolean friendNotificationsEnabled, boolean friendMessageNotificationsEnabled,
                           boolean friendMessagesEnabled) {
        this.uuid = uuid;
        this.status = status;
        this.friendRequestsEnabled = friendRequestsEnabled;
        this.friendNotificationsEnabled = friendNotificationsEnabled;
        this.friendMessageNotificationsEnabled = friendMessageNotificationsEnabled;
        this.friendMessagesEnabled = friendMessagesEnabled;
    }

    public UUID getUuid() {
        return uuid;
    }

    public FriendStatus getStatus() {
        return status;
    }

    public void setStatus(FriendStatus status) {
        this.status = status;
    }

    public boolean isFriendRequestsEnabled() {
        return friendRequestsEnabled;
    }

    public void setFriendRequestsEnabled(boolean friendRequestsEnabled) {
        this.friendRequestsEnabled = friendRequestsEnabled;
    }

    public boolean isFriendNotificationsEnabled() {
        return friendNotificationsEnabled;
    }

    public void setFriendNotificationsEnabled(boolean friendNotificationsEnabled) {
        this.friendNotificationsEnabled = friendNotificationsEnabled;
    }

    public boolean isFriendMessageNotificationsEnabled() {
        return friendMessageNotificationsEnabled;
    }

    public void setFriendMessageNotificationsEnabled(boolean friendMessageNotificationsEnabled) {
        this.friendMessageNotificationsEnabled = friendMessageNotificationsEnabled;
    }

    public boolean isFriendMessagesEnabled() {
        return friendMessagesEnabled;
    }

    public void setFriendMessagesEnabled(boolean friendMessagesEnabled) {
        this.friendMessagesEnabled = friendMessagesEnabled;
    }
}
