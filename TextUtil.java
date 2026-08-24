package com.friends.plugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Central helper for converting legacy '&'-coded strings into Adventure Components,
 * avoiding the deprecated org.bukkit.ChatColor APIs.
 */
public class TextUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {
    }

    /**
     * Converts a legacy '&'-coded string (e.g. "&aHello") into an Adventure Component.
     */
    public static Component legacy(String input) {
        if (input == null) input = "";
        return LEGACY.deserialize(input);
    }

    /**
     * Converts a legacy '&'-coded string into a plain string with section-sign (§) codes,
     * for APIs that still only accept String (e.g. chat messages).
     */
    public static String color(String input) {
        if (input == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(legacy(input));
    }
}
