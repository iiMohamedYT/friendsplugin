package com.friends.plugin.manager;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.database.FriendsDAO;
import com.friends.plugin.model.FriendEntry;
import com.friends.plugin.model.FriendStatus;
import com.friends.plugin.model.PlayerSettings;
import com.friends.plugin.redis.RedisMessage;
import com.google.gson.Gson;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FriendManager {

    private final FriendsPlugin plugin;
    private final FriendsDAO dao;
    private final Gson gson = new Gson();

    // Cache of player settings while online, to reduce DB calls
    private final Map<UUID, PlayerSettings> settingsCache = new ConcurrentHashMap<>();

    public FriendManager(FriendsPlugin plugin) {
        this.plugin = plugin;
        this.dao = plugin.getFriendsDAO();
    }

    // ================= SETTINGS =================

    public PlayerSettings getSettings(UUID uuid) {
        return settingsCache.get(uuid);
    }

    public void loadSettings(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                PlayerSettings settings = dao.getOrCreateSettings(player.getUniqueId(), player.getName());
                settingsCache.put(player.getUniqueId(), settings);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load settings for " + player.getName(), e);
            }
        });
    }

    public void unloadSettings(UUID uuid) {
        settingsCache.remove(uuid);
    }

    public void invalidateStatusCache(UUID uuid) {
        // Placeholder for future caching of remote statuses if needed
    }

    // ================= ONLINE STATE =================

    public void markOnline(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                dao.setOnlineState(player.getUniqueId(), player.getName(), true, plugin.getConfigManager().getServerName());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to mark player online", e);
            }
            broadcastPlayerOnline(player);
        });
    }

    public void markOffline(Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                dao.setOnlineState(player.getUniqueId(), player.getName(), false, null);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to mark player offline", e);
            }
            broadcastPlayerOffline(player);
        });
    }

    private void broadcastPlayerOnline(Player player) {
        RedisMessage msg = new RedisMessage("PLAYER_ONLINE", plugin.getConfigManager().getServerName(),
                player.getUniqueId().toString(), player.getName(), null, null, null);
        plugin.getRedisManager().publish(gson.toJson(msg));
        // Also notify friends that are on THIS server
        notifyFriendsOfStatusChange(player.getUniqueId(), player.getName(), true, plugin.getConfigManager().getServerName());
    }

    private void broadcastPlayerOffline(Player player) {
        RedisMessage msg = new RedisMessage("PLAYER_OFFLINE", plugin.getConfigManager().getServerName(),
                player.getUniqueId().toString(), player.getName(), null, null, null);
        plugin.getRedisManager().publish(gson.toJson(msg));
        notifyFriendsOfStatusChange(player.getUniqueId(), player.getName(), false, null);
    }

    /**
     * Notifies any online friends (on this server) that `uuid` changed online status.
     */
    public void notifyFriendsOfStatusChange(UUID uuid, String name, boolean online, String server) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<FriendEntry> friends = dao.getFriends(uuid);
                for (FriendEntry entry : friends) {
                    Player friendPlayer = plugin.getServer().getPlayer(entry.getFriendUuid());
                    if (friendPlayer == null || !friendPlayer.isOnline()) continue;

                    PlayerSettings settings = getSettings(friendPlayer.getUniqueId());
                    if (settings != null && !settings.isFriendNotificationsEnabled()) continue;

                    // Respect invisible status: if the player going online/offline is invisible, don't notify
                    FriendStatus theirStatus = dao.getStatus(uuid);
                    if (online && theirStatus == FriendStatus.INVISIBLE) continue;

                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        String key = online ? "friend-online" : "friend-offline";
                        friendPlayer.sendMessage(plugin.getConfigManager().getMessage(key, "%player%", name));
                    });
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to notify friends of status change", e);
            }
        });
    }

    // ================= STATUS =================

    public void cycleStatus(Player player) {
        PlayerSettings settings = getSettings(player.getUniqueId());
        if (settings == null) return;
        FriendStatus newStatus = settings.getStatus().next();
        settings.setStatus(newStatus);

        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                dao.updateStatus(player.getUniqueId(), newStatus);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update status", e);
            }
        });

        RedisMessage msg = new RedisMessage("STATUS_CHANGE", plugin.getConfigManager().getServerName(),
                player.getUniqueId().toString(), player.getName(), null, null, null);
        plugin.getRedisManager().publish(gson.toJson(msg));

        playSound(player, plugin.getConfigManager().getSoundStatusToggle());
    }

    // ================= FRIEND REQUESTS =================

    public void sendFriendRequest(Player sender, String targetName) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                if (sender.getName().equalsIgnoreCase(targetName)) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("cannot-add-self")));
                    return;
                }

                UUID targetUuid = dao.getUuidByUsername(targetName);
                if (targetUuid == null) {
                    // Try online player match (player might not have joined with tracked username yet)
                    Player online = plugin.getServer().getPlayerExact(targetName);
                    if (online != null) {
                        targetUuid = online.getUniqueId();
                    } else {
                        plugin.getServer().getScheduler().runTask(plugin, () ->
                                sender.sendMessage(plugin.getConfigManager().getMessage("player-not-found")));
                        return;
                    }
                }
                final UUID finalTargetUuid = targetUuid;
                String resolvedTargetName = dao.getUsernameByUuid(finalTargetUuid);
                if (resolvedTargetName == null) resolvedTargetName = targetName;

                if (dao.areFriends(sender.getUniqueId(), finalTargetUuid)) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("already-friends", "%player%", targetName)));
                    return;
                }

                if (dao.requestExists(sender.getUniqueId(), finalTargetUuid)) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("friend-request-already-sent", "%player%", targetName)));
                    return;
                }

                // If target already sent a request to sender, auto-accept instead
                if (dao.requestExists(finalTargetUuid, sender.getUniqueId())) {
                    acceptFriendRequestInternal(sender.getUniqueId(), sender.getName(), finalTargetUuid, resolvedTargetName);
                    return;
                }

                if (!dao.isFriendRequestsEnabled(finalTargetUuid)) {
                    String finalName = resolvedTargetName;
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("friend-requests-disabled-target", "%player%", finalName)));
                    return;
                }

                int friendCount = dao.getFriendCount(sender.getUniqueId());
                int max = plugin.getConfigManager().getMaxFriends();
                if (friendCount >= max) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sender.sendMessage(plugin.getConfigManager().getMessage("friend-list-full", "%max%", String.valueOf(max))));
                    return;
                }

                dao.createRequest(sender.getUniqueId(), finalTargetUuid);

                String finalResolvedName = resolvedTargetName;
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        sender.sendMessage(plugin.getConfigManager().getMessage("friend-request-sent", "%player%", finalResolvedName)));

                // Notify target: locally if online here, else via Redis
                Player targetOnline = plugin.getServer().getPlayer(finalTargetUuid);
                if (targetOnline != null && targetOnline.isOnline()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            sendFriendRequestChatPrompt(targetOnline, sender.getName(), sender.getUniqueId()));
                } else {
                    RedisMessage msg = new RedisMessage("FRIEND_REQUEST", plugin.getConfigManager().getServerName(),
                            sender.getUniqueId().toString(), sender.getName(),
                            finalTargetUuid.toString(), finalResolvedName, null);
                    plugin.getRedisManager().publish(gson.toJson(msg));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to send friend request", e);
            }
        });
    }

    /**
     * Sends a clickable chat message to the receiving player with Accept/Decline buttons.
     */
    public void sendFriendRequestChatPrompt(Player receiver, String senderName, UUID senderUuid) {
        receiver.sendMessage(plugin.getConfigManager().getMessage("friend-request-received", "%player%", senderName));

        Component accept = Component.text("[Accept]")
                .color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/friend accept " + senderName))
                .hoverEvent(HoverEvent.showText(Component.text("Click to accept " + senderName + "'s friend request")));

        Component space = Component.text("   ");

        Component decline = Component.text("[Decline]")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/friend decline " + senderName))
                .hoverEvent(HoverEvent.showText(Component.text("Click to decline " + senderName + "'s friend request")));

        receiver.sendMessage(Component.empty().append(accept).append(space).append(decline));
        playSound(receiver, plugin.getConfigManager().getSoundFriendRequest());
    }

    public void acceptFriendRequest(Player receiver, String senderName) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID senderUuid = dao.getUuidByUsername(senderName);
                if (senderUuid == null) {
                    Player online = plugin.getServer().getPlayerExact(senderName);
                    if (online != null) senderUuid = online.getUniqueId();
                }
                if (senderUuid == null || !dao.requestExists(senderUuid, receiver.getUniqueId())) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            receiver.sendMessage(plugin.getConfigManager().getMessage("player-not-found")));
                    return;
                }
                acceptFriendRequestInternal(senderUuid, dao.getUsernameByUuid(senderUuid), receiver.getUniqueId(), receiver.getName());
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to accept friend request", e);
            }
        });
    }

    private void acceptFriendRequestInternal(UUID senderUuid, String senderName, UUID receiverUuid, String receiverName) throws SQLException {
        int receiverFriendCount = dao.getFriendCount(receiverUuid);
        int senderFriendCount = dao.getFriendCount(senderUuid);
        int max = plugin.getConfigManager().getMaxFriends();

        Player receiverPlayer = plugin.getServer().getPlayer(receiverUuid);

        if (receiverFriendCount >= max) {
            if (receiverPlayer != null) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        receiverPlayer.sendMessage(plugin.getConfigManager().getMessage("friend-list-full", "%max%", String.valueOf(max))));
            }
            return;
        }
        if (senderFriendCount >= max) {
            if (receiverPlayer != null) {
                plugin.getServer().getScheduler().runTask(plugin, () ->
                        receiverPlayer.sendMessage(plugin.getConfigManager().getMessage("friend-list-full-other", "%player%", senderName)));
            }
            return;
        }

        dao.addFriendship(senderUuid, receiverUuid);
        dao.deleteRequest(senderUuid, receiverUuid);

        if (receiverPlayer != null && receiverPlayer.isOnline()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                receiverPlayer.sendMessage(plugin.getConfigManager().getMessage("friend-request-accepted", "%player%", senderName));
                playSound(receiverPlayer, plugin.getConfigManager().getSoundFriendRequest());
            });
        }

        Player senderPlayer = plugin.getServer().getPlayer(senderUuid);
        if (senderPlayer != null && senderPlayer.isOnline()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                senderPlayer.sendMessage(plugin.getConfigManager().getMessage("friend-request-accepted", "%player%", receiverName));
                playSound(senderPlayer, plugin.getConfigManager().getSoundFriendRequest());
            });
        } else {
            RedisMessage msg = new RedisMessage("FRIEND_REQUEST_ACCEPTED", plugin.getConfigManager().getServerName(),
                    receiverUuid.toString(), receiverName, senderUuid.toString(), senderName, null);
            plugin.getRedisManager().publish(gson.toJson(msg));
        }
    }

    public void declineFriendRequest(Player receiver, String senderName) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID senderUuid = dao.getUuidByUsername(senderName);
                if (senderUuid == null) {
                    Player online = plugin.getServer().getPlayerExact(senderName);
                    if (online != null) senderUuid = online.getUniqueId();
                }
                if (senderUuid == null || !dao.requestExists(senderUuid, receiver.getUniqueId())) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            receiver.sendMessage(plugin.getConfigManager().getMessage("player-not-found")));
                    return;
                }

                dao.deleteRequest(senderUuid, receiver.getUniqueId());

                plugin.getServer().getScheduler().runTask(plugin, () ->
                        receiver.sendMessage(plugin.getConfigManager().getMessage("friend-request-declined", "%player%", senderName)));

                Player senderPlayer = plugin.getServer().getPlayer(senderUuid);
                if (senderPlayer != null && senderPlayer.isOnline()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            senderPlayer.sendMessage(plugin.getConfigManager().getMessage("friend-request-declined", "%player%", receiver.getName())));
                } else {
                    RedisMessage msg = new RedisMessage("FRIEND_REQUEST_DECLINED", plugin.getConfigManager().getServerName(),
                            receiver.getUniqueId().toString(), receiver.getName(), senderUuid.toString(), senderName, null);
                    plugin.getRedisManager().publish(gson.toJson(msg));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to decline friend request", e);
            }
        });
    }

    public void removeFriend(Player player, String friendName) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                UUID friendUuid = dao.getUuidByUsername(friendName);
                if (friendUuid == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            player.sendMessage(plugin.getConfigManager().getMessage("player-not-found")));
                    return;
                }
                dao.removeFriendship(player.getUniqueId(), friendUuid);

                plugin.getServer().getScheduler().runTask(plugin, () ->
                        player.sendMessage(plugin.getConfigManager().getMessage("friend-removed", "%player%", friendName)));

                Player friendPlayer = plugin.getServer().getPlayer(friendUuid);
                if (friendPlayer != null && friendPlayer.isOnline()) {
                    plugin.getServer().getScheduler().runTask(plugin, () ->
                            friendPlayer.sendMessage(plugin.getConfigManager().getMessage("friend-removed", "%player%", player.getName())));
                } else {
                    RedisMessage msg = new RedisMessage("FRIEND_REMOVED", plugin.getConfigManager().getServerName(),
                            player.getUniqueId().toString(), player.getName(), friendUuid.toString(), friendName, null);
                    plugin.getRedisManager().publish(gson.toJson(msg));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to remove friend", e);
            }
        });
    }

    // ================= UTIL =================

    public void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1f, 1f);
        } catch (Exception ignored) {
        }
    }

    public FriendsDAO getDao() {
        return dao;
    }
}
