package com.friends.plugin.model;

import java.util.UUID;

/**
 * Represents a friend entry as seen from the perspective of one player.
 */
public class FriendEntry {

    private final UUID friendUuid;
    private String friendName;
    private final long addedAt;
    private long lastSeen; // epoch millis, updated when friend goes offline
    private boolean online;
    private String currentServer; // which server they're online on (cross-server)
    private FriendStatus status;

    public FriendEntry(UUID friendUuid, String friendName, long addedAt, long lastSeen,
                        boolean online, String currentServer, FriendStatus status) {
        this.friendUuid = friendUuid;
        this.friendName = friendName;
        this.addedAt = addedAt;
        this.lastSeen = lastSeen;
        this.online = online;
        this.currentServer = currentServer;
        this.status = status;
    }

    public UUID getFriendUuid() {
        return friendUuid;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public void setCurrentServer(String currentServer) {
        this.currentServer = currentServer;
    }

    public FriendStatus getStatus() {
        return status;
    }

    public void setStatus(FriendStatus status) {
        this.status = status;
    }

    /**
     * Whether this friend should be visually shown as "online" to the viewer,
     * respecting their invisible status.
     */
    public boolean isVisibleOnline() {
        return online && status != FriendStatus.INVISIBLE;
    }
}
