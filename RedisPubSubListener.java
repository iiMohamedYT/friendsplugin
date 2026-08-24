package com.friends.plugin.redis;

import com.friends.plugin.FriendsPlugin;
import com.google.gson.Gson;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.logging.Level;

public class RedisPubSubListener extends JedisPubSub {

    private final FriendsPlugin plugin;
    private final Gson gson = new Gson();

    public RedisPubSubListener(FriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            RedisMessage msg = gson.fromJson(message, RedisMessage.class);
            if (msg == null || msg.getType() == null) return;

            // Ignore messages that originated from this same server (already handled locally)
            if (plugin.getConfigManager().getServerName().equals(msg.getOriginServer())) return;

            // Process on the main thread since we touch Bukkit API
            plugin.getServer().getScheduler().runTask(plugin, () -> handleMessage(msg));
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to process Redis message", e);
        }
    }

    private void handleMessage(RedisMessage msg) {
        switch (msg.getType()) {
            case "PLAYER_ONLINE" -> handlePlayerOnline(msg);
            case "PLAYER_OFFLINE" -> handlePlayerOffline(msg);
            case "FRIEND_REQUEST" -> handleFriendRequest(msg);
            case "FRIEND_REQUEST_ACCEPTED" -> handleFriendRequestAccepted(msg);
            case "FRIEND_REQUEST_DECLINED" -> handleFriendRequestDeclined(msg);
            case "FRIEND_REMOVED" -> handleFriendRemoved(msg);
            case "PRIVATE_MESSAGE" -> handlePrivateMessage(msg);
            case "STATUS_CHANGE" -> handleStatusChange(msg);
            default -> { /* ignore unknown */ }
        }
    }

    private void handlePlayerOnline(RedisMessage msg) {
        UUID target = UUID.fromString(msg.getSenderUuid());
        String name = msg.getSenderName();
        plugin.getFriendManager().notifyFriendsOfStatusChange(target, name, true, msg.getOriginServer());
    }

    private void handlePlayerOffline(RedisMessage msg) {
        UUID target = UUID.fromString(msg.getSenderUuid());
        String name = msg.getSenderName();
        plugin.getFriendManager().notifyFriendsOfStatusChange(target, name, false, null);
    }

    private void handleFriendRequest(RedisMessage msg) {
        UUID receiverUuid = UUID.fromString(msg.getTargetUuid());
        Player receiver = plugin.getServer().getPlayer(receiverUuid);
        if (receiver == null || !receiver.isOnline()) return;
        plugin.getFriendManager().sendFriendRequestChatPrompt(receiver, msg.getSenderName(), UUID.fromString(msg.getSenderUuid()));
    }

    private void handleFriendRequestAccepted(RedisMessage msg) {
        UUID targetUuid = UUID.fromString(msg.getTargetUuid());
        Player target = plugin.getServer().getPlayer(targetUuid);
        if (target == null || !target.isOnline()) return;
        target.sendMessage(plugin.getConfigManager().getMessage("friend-request-accepted", "%player%", msg.getSenderName()));
        target.playSound(target.getLocation(), safeSound(plugin.getConfigManager().getSoundFriendRequest()), 1f, 1f);
    }

    private void handleFriendRequestDeclined(RedisMessage msg) {
        UUID targetUuid = UUID.fromString(msg.getTargetUuid());
        Player target = plugin.getServer().getPlayer(targetUuid);
        if (target == null || !target.isOnline()) return;
        target.sendMessage(plugin.getConfigManager().getMessage("friend-request-declined", "%player%", msg.getSenderName()));
    }

    private void handleFriendRemoved(RedisMessage msg) {
        UUID targetUuid = UUID.fromString(msg.getTargetUuid());
        Player target = plugin.getServer().getPlayer(targetUuid);
        if (target == null || !target.isOnline()) return;
        target.sendMessage(plugin.getConfigManager().getMessage("friend-removed", "%player%", msg.getSenderName()));
    }

    private void handlePrivateMessage(RedisMessage msg) {
        UUID targetUuid = UUID.fromString(msg.getTargetUuid());
        Player target = plugin.getServer().getPlayer(targetUuid);
        if (target == null || !target.isOnline()) return;

        plugin.getMessageManager().deliverIncomingMessage(target, msg.getSenderName(), UUID.fromString(msg.getSenderUuid()), msg.getContent());
    }

    private void handleStatusChange(RedisMessage msg) {
        UUID target = UUID.fromString(msg.getSenderUuid());
        plugin.getFriendManager().invalidateStatusCache(target);
    }

    private Sound safeSound(String name) {
        try {
            return Sound.valueOf(name);
        } catch (Exception e) {
            return Sound.UI_BUTTON_CLICK;
        }
    }
}
