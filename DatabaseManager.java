package com.friends.plugin.database;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.util.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class DatabaseManager {

    private final FriendsPlugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(FriendsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        ConfigManager cfg = plugin.getConfigManager();
        try {
            HikariConfig hikariConfig = new HikariConfig();
            String jdbcUrl = "jdbc:mysql://" + cfg.getMysqlHost() + ":" + cfg.getMysqlPort()
                    + "/" + cfg.getMysqlDatabase()
                    + "?useSSL=" + cfg.getMysqlUseSSL()
                    + "&autoReconnect=true&useUnicode=true&characterEncoding=utf8"
                    + "&serverTimezone=UTC";

            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(cfg.getMysqlUsername());
            hikariConfig.setPassword(cfg.getMysqlPassword());
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            hikariConfig.setMaximumPoolSize(cfg.getMysqlPoolSize());
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setPoolName("FriendsPlugin-Pool");
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(hikariConfig);

            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database!", e);
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Player settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS friends_settings (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    status VARCHAR(16) NOT NULL DEFAULT 'ONLINE',
                    friend_requests_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    friend_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    friend_message_notifications_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    friend_messages_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    last_seen BIGINT NOT NULL DEFAULT 0,
                    current_server VARCHAR(64) DEFAULT NULL,
                    is_online BOOLEAN NOT NULL DEFAULT FALSE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Friendships table (store one row per pair, uuid_a < uuid_b to avoid duplicates)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS friends_relations (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    uuid_a VARCHAR(36) NOT NULL,
                    uuid_b VARCHAR(36) NOT NULL,
                    added_at BIGINT NOT NULL,
                    UNIQUE KEY unique_pair (uuid_a, uuid_b),
                    INDEX idx_uuid_a (uuid_a),
                    INDEX idx_uuid_b (uuid_b)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Friend requests table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS friends_requests (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    sender_uuid VARCHAR(36) NOT NULL,
                    receiver_uuid VARCHAR(36) NOT NULL,
                    created_at BIGINT NOT NULL,
                    UNIQUE KEY unique_request (sender_uuid, receiver_uuid),
                    INDEX idx_receiver (receiver_uuid),
                    INDEX idx_sender (sender_uuid)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Sort preference table (per-player GUI sorting choice)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS friends_sort_prefs (
                    uuid VARCHAR(36) PRIMARY KEY,
                    sort_mode VARCHAR(16) NOT NULL DEFAULT 'DEFAULT',
                    sort_order VARCHAR(16) NOT NULL DEFAULT 'NORMAL'
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Data source is not initialized or is closed.");
        }
        return dataSource.getConnection();
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
