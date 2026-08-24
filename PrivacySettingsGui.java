package com.friends.plugin.gui;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.model.FriendStatus;
import com.friends.plugin.model.PlayerSettings;
import com.friends.plugin.util.ItemBuilder;
import com.friends.plugin.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Privacy settings GUI.
 * slot 10 -> Status (Online / Invisible / DND)
 * slot 12 -> Friend Requests toggle
 * slot 14 -> Friend Notifications toggle (join/leave)
 * slot 16 -> Friend Message Notifications toggle (sound)
 * slot 22 -> Friend Messages toggle (allow friends to message you)
 * slot 40 -> Back to main GUI
 */
public class PrivacySettingsGui implements FriendsGuiHolder {

    public static final int SIZE = 45;
    public static final int SLOT_STATUS = 10;
    public static final int SLOT_FRIEND_REQUESTS = 12;
    public static final int SLOT_FRIEND_NOTIFICATIONS = 14;
    public static final int SLOT_MESSAGE_NOTIFICATIONS = 16;
    public static final int SLOT_FRIEND_MESSAGES = 22;
    public static final int SLOT_BACK = 40;

    private final FriendsPlugin plugin;
    private final Player viewer;
    private Inventory inventory;

    public PrivacySettingsGui(FriendsPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    @Override
    public GuiType getType() {
        return GuiType.PRIVACY;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        inventory = plugin.getServer().createInventory(this, SIZE, TextUtil.legacy("&8Privacy Settings"));
        populate();
        viewer.openInventory(inventory);
    }

    public void refresh() {
        if (inventory == null) return;
        populate();
    }

    private void populate() {
        PlayerSettings settings = plugin.getFriendManager().getSettings(viewer.getUniqueId());
        if (settings == null) {
            settings = new PlayerSettings(viewer.getUniqueId());
        }

        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Status
        Material statusMat = switch (settings.getStatus()) {
            case ONLINE -> Material.LIME_DYE;
            case INVISIBLE -> Material.GRAY_DYE;
            case DND -> Material.RED_DYE;
        };
        inventory.setItem(SLOT_STATUS, new ItemBuilder(statusMat)
                .name("&e&lStatus: " + settings.getStatus().display())
                .lore(
                        "",
                        "&7Control how you appear to friends.",
                        "",
                        (settings.getStatus() == FriendStatus.ONLINE ? "&a> " : "&7  ") + "Online",
                        (settings.getStatus() == FriendStatus.INVISIBLE ? "&a> " : "&7  ") + "Invisible",
                        (settings.getStatus() == FriendStatus.DND ? "&a> " : "&7  ") + "Do Not Disturb",
                        "",
                        "&eClick to cycle status"
                )
                .build());

        // Friend requests toggle
        inventory.setItem(SLOT_FRIEND_REQUESTS, toggleItem(
                Material.PAPER,
                "&e&lFriend Requests",
                "&7Allow other players to send", "&7you friend requests.",
                settings.isFriendRequestsEnabled()
        ));

        // Friend notifications toggle
        inventory.setItem(SLOT_FRIEND_NOTIFICATIONS, toggleItem(
                Material.BELL,
                "&e&lFriend Notifications",
                "&7Get notified in chat when", "&7a friend joins or leaves.",
                settings.isFriendNotificationsEnabled()
        ));

        // Message notifications toggle
        inventory.setItem(SLOT_MESSAGE_NOTIFICATIONS, toggleItem(
                Material.NOTE_BLOCK,
                "&e&lMessage Notifications",
                "&7Play a sound when a friend", "&7sends you a message.",
                settings.isFriendMessageNotificationsEnabled()
        ));

        // Friend messages toggle
        inventory.setItem(SLOT_FRIEND_MESSAGES, toggleItem(
                Material.WRITABLE_BOOK,
                "&e&lFriend Messages",
                "&7Allow your friends to send", "&7you private messages.",
                settings.isFriendMessagesEnabled()
        ));

        // Back button
        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name("&c&lBack")
                .lore("&7Return to the friends list")
                .build());
    }

    private ItemStack toggleItem(Material material, String name, String loreLine1, String loreLine2, boolean enabled) {
        return new ItemBuilder(material)
                .name(name)
                .lore(
                        loreLine1,
                        loreLine2,
                        "",
                        "&7Status: " + (enabled ? "&aEnabled" : "&cDisabled"),
                        "",
                        "&eClick to toggle"
                )
                .build();
    }
}
