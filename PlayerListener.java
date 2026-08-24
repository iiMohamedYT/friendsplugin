package com.friends.plugin.listener;

import com.friends.plugin.FriendsPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final FriendsPlugin plugin;

    public PlayerListener(FriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        plugin.getFriendManager().loadSettings(event.getPlayer());
        // Slight delay to ensure settings are loaded/created before marking online & notifying friends
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                plugin.getFriendManager().markOnline(event.getPlayer()), 20L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getFriendManager().markOffline(event.getPlayer());
        plugin.getFriendManager().unloadSettings(event.getPlayer().getUniqueId());
    }
}
