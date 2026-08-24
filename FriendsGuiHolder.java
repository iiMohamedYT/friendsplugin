package com.friends.plugin.gui;

import org.bukkit.inventory.InventoryHolder;

public interface FriendsGuiHolder extends InventoryHolder {
    GuiType getType();

    enum GuiType {
        MAIN,
        PRIVACY,
        REQUESTS,
        REQUEST_ACTION
    }
}
