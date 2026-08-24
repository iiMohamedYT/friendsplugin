package com.friends.plugin.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration raw() {
        return config;
    }

    public String getServerName() {
        return config.getString("server-name", "server1");
    }

    // ---- MySQL ----
    public String getMysqlHost() { return config.getString("mysql.host", "localhost"); }
    public int getMysqlPort() { return config.getInt("mysql.port", 3306); }
    public String getMysqlDatabase() { return config.getString("mysql.database", "friends_db"); }
    public String getMysqlUsername() { return config.getString("mysql.username", "root"); }
    public String getMysqlPassword() { return config.getString("mysql.password", ""); }
    public boolean getMysqlUseSSL() { return config.getBoolean("mysql.useSSL", false); }
    public int getMysqlPoolSize() { return config.getInt("mysql.pool-size", 10); }

    // ---- Redis ----
    public boolean isRedisEnabled() { return config.getBoolean("redis.enabled", true); }
    public String getRedisHost() { return config.getString("redis.host", "localhost"); }
    public int getRedisPort() { return config.getInt("redis.port", 6379); }
    public String getRedisPassword() { return config.getString("redis.password", ""); }
    public int getRedisDatabase() { return config.getInt("redis.database", 0); }
    public String getRedisChannel() { return config.getString("redis.channel", "friendsplugin:sync"); }

    // ---- Settings ----
    public int getMaxFriends() { return config.getInt("settings.max-friends", 30); }
    public int getRequestExpireDays() { return config.getInt("settings.request-expire-days", 14); }
    public String getSoundMessageReceived() { return config.getString("settings.sound.message-received", "ENTITY_EXPERIENCE_ORB_PICKUP"); }
    public String getSoundFriendRequest() { return config.getString("settings.sound.friend-request", "ENTITY_PLAYER_LEVELUP"); }
    public String getSoundStatusToggle() { return config.getString("settings.sound.status-toggle", "UI_BUTTON_CLICK"); }

    // ---- Messages ----
    public String getPrefix() {
        String prefix = config.getString("messages.prefix", "&8[&bFriends&8] &r");
        return TextUtil.color(prefix);
    }

    public String getMessage(String key) {
        String prefix = config.getString("messages.prefix", "&8[&bFriends&8] &r");
        String msg = config.getString("messages." + key, "");
        msg = msg.replace("%prefix%", prefix);
        return TextUtil.color(msg);
    }

    public String getMessage(String key, String... replacements) {
        String msg = getMessage(key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return msg;
    }
}
