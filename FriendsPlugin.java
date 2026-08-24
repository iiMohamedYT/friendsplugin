package com.friends.plugin;

import com.friends.plugin.command.FmsgCommand;
import com.friends.plugin.command.FriendCommand;
import com.friends.plugin.database.DatabaseManager;
import com.friends.plugin.database.FriendsDAO;
import com.friends.plugin.listener.GuiListener;
import com.friends.plugin.listener.PlayerListener;
import com.friends.plugin.manager.FriendManager;
import com.friends.plugin.manager.MessageManager;
import com.friends.plugin.redis.RedisManager;
import com.friends.plugin.util.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FriendsPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private FriendsDAO friendsDAO;
    private RedisManager redisManager;
    private FriendManager friendManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);

        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Could not connect to MySQL database! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.friendsDAO = new FriendsDAO(this);
        this.redisManager = new RedisManager(this);
        redisManager.connect();

        this.friendManager = new FriendManager(this);
        this.messageManager = new MessageManager(this);

        // Register commands
        getCommand("friend").setExecutor(new FriendCommand(this));
        getCommand("friend").setTabCompleter(new FriendCommand(this));
        getCommand("fmsg").setExecutor(new FmsgCommand(this));
        getCommand("fmsg").setTabCompleter(new FmsgCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Load settings for any already-online players (e.g. /reload)
        for (Player player : getServer().getOnlinePlayers()) {
            friendManager.loadSettings(player);
            friendManager.markOnline(player);
        }

        getLogger().info("FriendsPlugin has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (friendManager != null) {
            for (Player player : getServer().getOnlinePlayers()) {
                friendManager.markOffline(player);
            }
        }
        if (redisManager != null) {
            redisManager.close();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("FriendsPlugin has been disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public FriendsDAO getFriendsDAO() {
        return friendsDAO;
    }

    public RedisManager getRedisManager() {
        return redisManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
