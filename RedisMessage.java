package com.friends.plugin.redis;

/**
 * Payload sent over Redis pub/sub between servers.
 * type values: PLAYER_ONLINE, PLAYER_OFFLINE, FRIEND_REQUEST, FRIEND_REQUEST_ACCEPTED,
 *              FRIEND_REQUEST_DECLINED, FRIEND_REMOVED, PRIVATE_MESSAGE, STATUS_CHANGE
 */
public class RedisMessage {

    private String type;
    private String originServer;
    private String senderUuid;
    private String senderName;
    private String targetUuid;
    private String targetName;
    private String content; // used for private messages
    private long timestamp;

    public RedisMessage() {
    }

    public RedisMessage(String type, String originServer, String senderUuid, String senderName,
                         String targetUuid, String targetName, String content) {
        this.type = type;
        this.originServer = originServer;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOriginServer() { return originServer; }
    public void setOriginServer(String originServer) { this.originServer = originServer; }

    public String getSenderUuid() { return senderUuid; }
    public void setSenderUuid(String senderUuid) { this.senderUuid = senderUuid; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTargetUuid() { return targetUuid; }
    public void setTargetUuid(String targetUuid) { this.targetUuid = targetUuid; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
