package com.friends.plugin.model;

import java.util.UUID;

public class FriendRequest {

    private final UUID sender;
    private final UUID receiver;
    private final long timestamp;

    public FriendRequest(UUID sender, UUID receiver, long timestamp) {
        this.sender = sender;
        this.receiver = receiver;
        this.timestamp = timestamp;
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
