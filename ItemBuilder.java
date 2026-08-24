package com.friends.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder(ItemStack base) {
        this.itemStack = base.clone();
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null) {
            Component component = TextUtil.legacy(name).decoration(TextDecoration.ITALIC, false);
            meta.displayName(component);
        }
        return this;
    }

    public ItemBuilder lore(String... lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(TextUtil.legacy(line).decoration(TextDecoration.ITALIC, false));
        }
        if (meta != null) {
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        List<Component> lore = new ArrayList<>();
        for (String line : lines) {
            lore.add(TextUtil.legacy(line).decoration(TextDecoration.ITALIC, false));
        }
        if (meta != null) {
            meta.lore(lore);
        }
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(Math.max(1, amount));
        return this;
    }

    public ItemBuilder skullOwner(UUID uuid) {
        if (meta instanceof SkullMeta skullMeta) {
            org.bukkit.OfflinePlayer offlinePlayer = org.bukkit.Bukkit.getOfflinePlayer(uuid);
            skullMeta.setOwningPlayer(offlinePlayer);
        }
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}
