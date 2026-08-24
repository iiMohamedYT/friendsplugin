package com.friends.plugin.manager;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.database.FriendsDAO;
import com.friends.plugin.model.PlayerSettings;
import com.friends.plugin.redis.RedisMessage;
import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class MessageManager {

    private final FriendsPlugin plugin;
    private final FriendsDAO dao;
    private final Gson gson = new Gson();

    // Tracks last message partner for potential /r reply support later
    public MessageManager(FriendsPlugin plugin) {
        this.plugin = plugin;
        this.dao = plugin.getFriendsDAO();
    }

    public void sendMessage(Player sender, String targetName, String content) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID targetUuid = dao.getUuidByUsername(targetName);
                if (targetUuid == null) {
                    Player online = plugin.getServer().getPlayerExact(targetName);
                    if (online != null) targetUuid = online.getUniqueId();
                }
                if (targetUuid == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found")));
                    return;
                }

                if (!dao.areFriends(sender.getUniqueId(), targetUuid)) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("not-friends-cannot-message")));
                    return;
                }

                if (!dao.isFriendMessagesEnabled(targetUuid)) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("messages-disabled")));
                    return;
                }

                String resolvedTargetName = dao.getUsernameByUuid(targetUuid);
                final String finalTargetName = resolvedTargetName != null ? resolvedTargetName : targetName;
                final UUID finalTargetUuid = targetUuid;

                // Echo to sender
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage(Component.text("[You -> " + finalTargetName + "]: ", NamedTextColor.GRAY)
                                .append(Component.text(content, NamedTextColor.WHITE))));

                Player targetPlayer = plugin.getServer().getPlayer(finalTargetUuid);
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            deliverIncomingMessage(targetPlayer, sender.getName(), sender.getUniqueId(), content));
                } else {
                    // Send via Redis to whichever server the target is on
                    RedisMessage msg = new RedisMessage("PRIVATE_MESSAGE", plugin.getConfigManager().getServerName(),
                            sender.getUniqueId().toString(), sender.getName(),
                            finalTargetUuid.toString(), finalTargetName, content);
                    plugin.getRedisManager().publish(gson.toJson(msg));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send friend message", e);
            }
        });
    }

    /**
     * Called when this server needs to display an incoming message to a locally-connected player,
     * either because it originated here or arrived via Redis from another server.
     */
    public void deliverIncomingMessage(Player target, String senderName, UUID senderUuid, String content) {
        target.sendMessage(Component.text("[" + senderName + " -> You]: ", NamedTextColor.GRAY)
                .append(Component.text(content, NamedTextColor.WHITE)));

        PlayerSettings settings = plugin.getFriendManager().getSettings(target.getUniqueId());
        boolean playSound = settings == null || settings.isFriendMessageNotificationsEnabled();
        if (playSound) {
            plugin.getFriendManager().playSound(target, plugin.getConfigManager().getSoundMessageReceived());
        }
    }
}
