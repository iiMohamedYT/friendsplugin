package com.friends.plugin.redis;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.util.ConfigManager;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.logging.Level;

public class RedisManager {

    private final FriendsPlugin plugin;
    private JedisPool jedisPool;
    private Thread subscriberThread;
    private RedisPubSubListener pubSubListener;
    private volatile boolean enabled;

    public RedisManager(FriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        ConfigManager cfg = plugin.getConfigManager();
        enabled = cfg.isRedisEnabled();
        if (!enabled) {
            plugin.getLogger().info("Redis is disabled in config. Cross-server sync via Redis will not be used.");
            return false;
        }

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(8);
            poolConfig.setMaxIdle(4);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);

            String password = cfg.getRedisPassword();
            if (password == null || password.isEmpty()) {
                jedisPool = new JedisPool(poolConfig, cfg.getRedisHost(), cfg.getRedisPort(), 5000);
            } else {
                jedisPool = new JedisPool(poolConfig, cfg.getRedisHost(), cfg.getRedisPort(), 5000, password, cfg.getRedisDatabase());
            }

            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            startSubscriber();
            plugin.getLogger().info("Connected to Redis successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to Redis! Cross-server features will not work.", e);
            enabled = false;
            return false;
        }
    }

    private void startSubscriber() {
        pubSubListener = new RedisPubSubListener(plugin);
        subscriberThread = new Thread(() -> {
            while (enabled && jedisPool != null && !jedisPool.isClosed()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(pubSubListener, plugin.getConfigManager().getRedisChannel());
                } catch (Exception e) {
                    if (enabled) {
                        plugin.getLogger().warning("Redis subscriber connection lost, retrying in 5s...");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }
        }, "FriendsPlugin-Redis-Subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }

    public void publish(String message) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) return;
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.publish(plugin.getConfigManager().getRedisChannel(), message);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to publish Redis message", e);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void close() {
        enabled = false;
        try {
            if (pubSubListener != null && pubSubListener.isSubscribed()) {
                pubSubListener.unsubscribe();
            }
        } catch (Exception ignored) {}

        if (subscriberThread != null) {
            subscriberThread.interrupt();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}
