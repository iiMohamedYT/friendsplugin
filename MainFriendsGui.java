package com.friends.plugin.gui;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.model.FriendEntry;
import com.friends.plugin.util.ItemBuilder;
import com.friends.plugin.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

/**
 * Main /friend GUI.
 * Rows 0-4 (45 slots): friend list entries (player heads)
 * Row 5 (slots 45-53): control buttons - Add Friend, Requests, Sorting, Info, Privacy, Remove-hint, ..., Close
 *
 * Layout mirrors the screenshots:
 *  slot 45 -> Add friend (chest / nether star like icon)
 *  slot 46 -> Friend Requests (red bed-like icon)
 *  slot 49 -> Change list sorting (anvil/observer)
 *  slot 50 -> Privacy settings (shield / book)
 *  slot 53 -> Friends Info (compass-like / ender chest)
 */
public class MainFriendsGui implements FriendsGuiHolder {

    public static final int SIZE = 54;
    public static final int SLOT_ADD_FRIEND = 45;
    public static final int SLOT_REQUESTS = 46;
    public static final int SLOT_SORTING = 49;
    public static final int SLOT_PRIVACY = 50;
    public static final int SLOT_INFO = 53;

    private final FriendsPlugin plugin;
    private final Player viewer;
    private Inventory inventory;

    public MainFriendsGui(FriendsPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    @Override
    public GuiType getType() {
        return GuiType.MAIN;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        inventory = plugin.getServer().createInventory(this, SIZE, TextUtil.legacy("&8Friends List"));
        populate();
        viewer.openInventory(inventory);
    }

    public void refresh() {
        if (inventory == null) return;
        populate();
    }

    private void populate() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                List<FriendEntry> friends = plugin.getFriendsDAO().getFriends(viewer.getUniqueId());
                String[] prefs = plugin.getFriendsDAO().getSortPrefs(viewer.getUniqueId());
                SortMode mode = SortMode.valueOf(prefs[0]);
                SortOrder order = SortOrder.valueOf(prefs[1]);

                sortFriends(friends, mode, order);

                int onlineCount = (int) friends.stream().filter(FriendEntry::isVisibleOnline).count();
                int total = friends.size();
                int max = plugin.getConfigManager().getMaxFriends();
                int requestCount = plugin.getFriendsDAO().getIncomingRequestCount(viewer.getUniqueId());

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (inventory == null) return;
                    fillFriendHeads(friends);
                    fillControlBar(mode, order, onlineCount, total, max, requestCount);
                });
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to populate friends GUI", e);
            }
        });
    }

    private void sortFriends(List<FriendEntry> friends, SortMode mode, SortOrder order) {
        Comparator<FriendEntry> comparator = switch (mode) {
            case ALPHABETICAL -> Comparator.comparing(f -> f.getFriendName().toLowerCase());
            case LAST_SEEN -> Comparator.comparingLong(FriendEntry::getLastSeen).reversed();
            case DEFAULT -> Comparator
                    .comparing((FriendEntry f) -> !f.isVisibleOnline()) // online first
                    .thenComparingLong(FriendEntry::getAddedAt);
        };
        friends.sort(comparator);
        if (order == SortOrder.REVERSED) {
            java.util.Collections.reverse(friends);
        }
    }

    private void fillFriendHeads(List<FriendEntry> friends) {
        int slot = 0;
        for (FriendEntry entry : friends) {
            if (slot >= 45) break; // only 45 slots for friend heads
            inventory.setItem(slot, buildFriendHead(entry));
            slot++;
        }
        // Clear remaining friend slots
        for (int i = slot; i < 45; i++) {
            inventory.setItem(i, null);
        }
    }

    private ItemStack buildFriendHead(FriendEntry entry) {
        List<String> lore = new ArrayList<>();

        boolean visibleOnline = entry.isVisibleOnline();
        lore.add(visibleOnline ? "&aOnline" + (entry.getCurrentServer() != null ? " &7(" + entry.getCurrentServer() + ")" : "") : "&7Offline");
        if (!visibleOnline) {
            lore.add("&7Last seen: &f" + formatDate(entry.getLastSeen()));
        }
        lore.add("");
        lore.add("&eLeft click &7to message");
        lore.add("&cShift-Right click &7to remove friend");

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(entry.getFriendUuid())
                .name((visibleOnline ? "&a" : "&7") + entry.getFriendName())
                .lore(lore)
                .build();
    }

    private String formatDate(long millis) {
        if (millis <= 0) return "Unknown";
        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(millis));
    }

    private void fillControlBar(SortMode mode, SortOrder order, int online, int total, int max, int requestCount) {
        // Fill the divider row with glass panes first
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Add friend
        inventory.setItem(SLOT_ADD_FRIEND, new ItemBuilder(Material.CHEST)
                .name("&6&lAdd Friend")
                .lore("&7Click and type: &f/friend add <player>", "", "&7Send a friend request to a player.")
                .build());

        // Friend requests
        List<String> reqLore = new ArrayList<>();
        reqLore.add("&7View and manage incoming");
        reqLore.add("&7friend requests.");
        reqLore.add("");
        reqLore.add(requestCount > 0 ? "&e" + requestCount + " pending request" + (requestCount == 1 ? "" : "s") : "&7No pending requests");
        reqLore.add("");
        reqLore.add("&aClick to open");
        inventory.setItem(SLOT_REQUESTS, new ItemBuilder(Material.RED_BED)
                .name("&c&lFriend Requests" + (requestCount > 0 ? " &e(" + requestCount + ")" : ""))
                .lore(reqLore)
                .build());

        // Sorting
        inventory.setItem(SLOT_SORTING, new ItemBuilder(Material.BLUE_ICE)
                .name("&b&lChange list sorting")
                .lore(
                        "",
                        "&aSorting: &f" + mode.getLabel(),
                        "&aOrder: &f" + (order == SortOrder.NORMAL ? "Normal" : "Reversed"),
                        "",
                        "&aDefault&7: " + splitDesc(SortMode.DEFAULT.getDescription()),
                        "&aAlphabetical&7: " + splitDesc(SortMode.ALPHABETICAL.getDescription()),
                        "&aLast seen&7: " + splitDesc(SortMode.LAST_SEEN.getDescription()),
                        "",
                        "&eClick to change sorting",
                        "&eShift click to reverse"
                )
                .build());

        // Privacy settings
        inventory.setItem(SLOT_PRIVACY, new ItemBuilder(Material.WHITE_BANNER)
                .name("&f&lPrivacy Settings")
                .lore("&7Control your status, requests,", "&7notifications and messages.", "", "&aClick to open")
                .build());

        // Info
        inventory.setItem(SLOT_INFO, new ItemBuilder(Material.ENDER_EYE)
                .name("&d&lFriends Info")
                .lore(
                        "",
                        "&aOnline: &f" + online,
                        "&aTotal: &f" + total,
                        "&aLimit: &f" + max,
                        "",
                        "&aRequests: &f" + requestCount
                )
                .build());
    }

    private String splitDesc(String desc) {
        return desc;
    }
}
