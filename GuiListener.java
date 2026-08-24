package com.friends.plugin.listener;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.gui.*;
import com.friends.plugin.model.PlayerSettings;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.sql.SQLException;
import java.util.logging.Level;

public class GuiListener implements Listener {

    private final FriendsPlugin plugin;

    public GuiListener(FriendsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FriendsGuiHolder holder)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Ignore clicks in the player's own inventory (bottom half); only handle the GUI (top) inventory
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        int slot = event.getSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        switch (holder.getType()) {
            case MAIN -> handleMainClick(player, (MainFriendsGui) holder, event, slot);
            case PRIVACY -> handlePrivacyClick(player, (PrivacySettingsGui) holder, event, slot);
            case REQUESTS -> handleRequestsClick(player, (FriendRequestsGui) holder, event, slot);
            default -> {}
        }
    }

    private void handleMainClick(Player player, MainFriendsGui gui, InventoryClickEvent event, int slot) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == MainFriendsGui.SLOT_ADD_FRIEND) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§eType: §f/friend add <player>");
            return;
        }

        if (slot == MainFriendsGui.SLOT_REQUESTS) {
            new FriendRequestsGui(plugin, player).open();
            return;
        }

        if (slot == MainFriendsGui.SLOT_SORTING) {
            handleSortingClick(player, gui, event.getClick());
            return;
        }

        if (slot == MainFriendsGui.SLOT_PRIVACY) {
            new PrivacySettingsGui(plugin, player).open();
            return;
        }

        if (slot == MainFriendsGui.SLOT_INFO) {
            return; // informational only
        }

        // Friend head clicked (slots 0-44)
        if (slot < 45 && clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            if (skullMeta.getOwningPlayer() == null) return;
            String friendName = skullMeta.getOwningPlayer().getName();
            if (friendName == null) return;

            if (event.getClick() == ClickType.SHIFT_RIGHT) {
                plugin.getFriendManager().removeFriend(player, friendName);
                player.closeInventory();
            } else if (event.getClick() == ClickType.LEFT) {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§eType: §f/fmsg " + friendName + " <message>");
            }
        }
    }

    private void handleSortingClick(Player player, MainFriendsGui gui, ClickType clickType) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String[] prefs = plugin.getFriendsDAO().getSortPrefs(player.getUniqueId());
                SortMode mode = SortMode.valueOf(prefs[0]);
                SortOrder order = SortOrder.valueOf(prefs[1]);

                if (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT) {
                    order = order.flip();
                } else {
                    mode = mode.next();
                }

                plugin.getFriendsDAO().setSortPrefs(player.getUniqueId(), mode.name(), order.name());
                plugin.getFriendManager().playSound(player, plugin.getConfigManager().getSoundStatusToggle());

                plugin.getServer().getScheduler().runTask(plugin, gui::refresh);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update sort prefs", e);
            }
        });
    }

    private void handlePrivacyClick(Player player, PrivacySettingsGui gui, InventoryClickEvent event, int slot) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == PrivacySettingsGui.SLOT_BACK) {
            new MainFriendsGui(plugin, player).open();
            return;
        }

        PlayerSettings settings = plugin.getFriendManager().getSettings(player.getUniqueId());
        if (settings == null) return;

        if (slot == PrivacySettingsGui.SLOT_STATUS) {
            plugin.getFriendManager().cycleStatus(player);
            gui.refresh();
            return;
        }

        if (slot == PrivacySettingsGui.SLOT_FRIEND_REQUESTS) {
            boolean newVal = !settings.isFriendRequestsEnabled();
            settings.setFriendRequestsEnabled(newVal);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getFriendsDAO().updateFriendRequestsEnabled(player.getUniqueId(), newVal);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to update friend requests toggle", e);
                }
            });
            plugin.getFriendManager().playSound(player, plugin.getConfigManager().getSoundStatusToggle());
            gui.refresh();
            return;
        }

        if (slot == PrivacySettingsGui.SLOT_FRIEND_NOTIFICATIONS) {
            boolean newVal = !settings.isFriendNotificationsEnabled();
            settings.setFriendNotificationsEnabled(newVal);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getFriendsDAO().updateFriendNotificationsEnabled(player.getUniqueId(), newVal);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to update friend notifications toggle", e);
                }
            });
            plugin.getFriendManager().playSound(player, plugin.getConfigManager().getSoundStatusToggle());
            gui.refresh();
            return;
        }

        if (slot == PrivacySettingsGui.SLOT_MESSAGE_NOTIFICATIONS) {
            boolean newVal = !settings.isFriendMessageNotificationsEnabled();
            settings.setFriendMessageNotificationsEnabled(newVal);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getFriendsDAO().updateFriendMessageNotificationsEnabled(player.getUniqueId(), newVal);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to update message notifications toggle", e);
                }
            });
            plugin.getFriendManager().playSound(player, plugin.getConfigManager().getSoundStatusToggle());
            gui.refresh();
            return;
        }

        if (slot == PrivacySettingsGui.SLOT_FRIEND_MESSAGES) {
            boolean newVal = !settings.isFriendMessagesEnabled();
            settings.setFriendMessagesEnabled(newVal);
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    plugin.getFriendsDAO().updateFriendMessagesEnabled(player.getUniqueId(), newVal);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to update friend messages toggle", e);
                }
            });
            plugin.getFriendManager().playSound(player, plugin.getConfigManager().getSoundStatusToggle());
            gui.refresh();
        }
    }

    private void handleRequestsClick(Player player, FriendRequestsGui gui, InventoryClickEvent event, int slot) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (slot == FriendRequestsGui.SLOT_BACK) {
            new MainFriendsGui(plugin, player).open();
            return;
        }

        if (slot < 45 && clicked.getType() == Material.PLAYER_HEAD && clicked.getItemMeta() instanceof SkullMeta skullMeta) {
            if (skullMeta.getOwningPlayer() == null) return;
            String senderName = skullMeta.getOwningPlayer().getName();
            if (senderName == null) return;

            if (event.getClick() == ClickType.LEFT) {
                plugin.getFriendManager().acceptFriendRequest(player, senderName);
                player.closeInventory();
            } else if (event.getClick() == ClickType.RIGHT) {
                plugin.getFriendManager().declineFriendRequest(player, senderName);
                // Refresh the requests list after a short delay to allow async op to complete
                plugin.getServer().getScheduler().runTaskLater(plugin, gui::refresh, 4L);
            }
        }
    }
}
