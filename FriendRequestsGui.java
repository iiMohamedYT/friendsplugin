package com.friends.plugin.gui;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.model.FriendRequest;
import com.friends.plugin.util.ItemBuilder;
import com.friends.plugin.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class FriendRequestsGui implements FriendsGuiHolder {

    public static final int SIZE = 54;
    public static final int SLOT_BACK = 49;

    private final FriendsPlugin plugin;
    private final Player viewer;
    private Inventory inventory;

    public FriendRequestsGui(FriendsPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.viewer = viewer;
    }

    @Override
    public GuiType getType() {
        return GuiType.REQUESTS;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        inventory = plugin.getServer().createInventory(this, SIZE, TextUtil.legacy("&8Friend Requests"));
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
                List<FriendRequest> requests = plugin.getFriendsDAO().getIncomingRequests(viewer.getUniqueId());
                plugin.getServer().getScheduler().runTask(plugin, () -> fill(requests));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load friend requests", e);
            }
        });
    }

    private void fill(List<FriendRequest> requests) {
        inventory.clear();
        ItemStack filler = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        int slot = 0;
        for (FriendRequest req : requests) {
            if (slot >= 45) break;
            String senderName = plugin.getServer().getOfflinePlayer(req.getSender()).getName();
            if (senderName == null) senderName = "Unknown";

            List<String> lore = new ArrayList<>();
            lore.add("&7Sent: &f" + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(req.getTimestamp())));
            lore.add("");
            lore.add("&aLeft click &7to accept");
            lore.add("&cRight click &7to decline");

            inventory.setItem(slot, new ItemBuilder(Material.PLAYER_HEAD)
                    .skullOwner(req.getSender())
                    .name("&e" + senderName)
                    .lore(lore)
                    .build());
            slot++;
        }

        inventory.setItem(SLOT_BACK, new ItemBuilder(Material.ARROW)
                .name("&c&lBack")
                .lore("&7Return to the friends list")
                .build());
    }
}
