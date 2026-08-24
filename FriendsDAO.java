package com.friends.plugin.database;

import com.friends.plugin.FriendsPlugin;
import com.friends.plugin.model.FriendEntry;
import com.friends.plugin.model.FriendRequest;
import com.friends.plugin.model.FriendStatus;
import com.friends.plugin.model.PlayerSettings;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendsDAO {

    private final DatabaseManager db;

    public FriendsDAO(FriendsPlugin plugin) {
        this.db = plugin.getDatabaseManager();
    }

    // ================= PLAYER SETTINGS =================

    public PlayerSettings getOrCreateSettings(UUID uuid, String username) throws SQLException {
        String select = "SELECT * FROM friends_settings WHERE uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSettings(rs);
                }
            }
        }
        // Doesn't exist, create it
        String insert = """
            INSERT INTO friends_settings (uuid, username, status, friend_requests_enabled,
                friend_notifications_enabled, friend_message_notifications_enabled,
                friend_messages_enabled, last_seen, is_online)
            VALUES (?, ?, 'ONLINE', TRUE, TRUE, TRUE, TRUE, ?, FALSE)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
        return new PlayerSettings(uuid);
    }

    private PlayerSettings mapSettings(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        FriendStatus status = FriendStatus.valueOf(rs.getString("status"));
        boolean requestsEnabled = rs.getBoolean("friend_requests_enabled");
        boolean notifEnabled = rs.getBoolean("friend_notifications_enabled");
        boolean msgNotifEnabled = rs.getBoolean("friend_message_notifications_enabled");
        boolean messagesEnabled = rs.getBoolean("friend_messages_enabled");
        return new PlayerSettings(uuid, status, requestsEnabled, notifEnabled, msgNotifEnabled, messagesEnabled);
    }

    public void updateStatus(UUID uuid, FriendStatus status) throws SQLException {
        String sql = "UPDATE friends_settings SET status = ? WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateFriendRequestsEnabled(UUID uuid, boolean enabled) throws SQLException {
        String sql = "UPDATE friends_settings SET friend_requests_enabled = ? WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateFriendNotificationsEnabled(UUID uuid, boolean enabled) throws SQLException {
        String sql = "UPDATE friends_settings SET friend_notifications_enabled = ? WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateFriendMessageNotificationsEnabled(UUID uuid, boolean enabled) throws SQLException {
        String sql = "UPDATE friends_settings SET friend_message_notifications_enabled = ? WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void updateFriendMessagesEnabled(UUID uuid, boolean enabled) throws SQLException {
        String sql = "UPDATE friends_settings SET friend_messages_enabled = ? WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    public void setOnlineState(UUID uuid, String username, boolean online, String serverName) throws SQLException {
        String sql = """
            UPDATE friends_settings SET is_online = ?, current_server = ?, username = ?, last_seen = ?
            WHERE uuid = ?
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, online);
            ps.setString(2, online ? serverName : null);
            ps.setString(3, username);
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, uuid.toString());
            ps.executeUpdate();
        }
    }

    public FriendStatus getStatus(UUID uuid) throws SQLException {
        String sql = "SELECT status FROM friends_settings WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return FriendStatus.valueOf(rs.getString("status"));
                }
            }
        }
        return FriendStatus.ONLINE;
    }

    public boolean isFriendMessagesEnabled(UUID uuid) throws SQLException {
        String sql = "SELECT friend_messages_enabled FROM friends_settings WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("friend_messages_enabled");
                }
            }
        }
        return true;
    }

    public boolean isFriendRequestsEnabled(UUID uuid) throws SQLException {
        String sql = "SELECT friend_requests_enabled FROM friends_settings WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("friend_requests_enabled");
                }
            }
        }
        return true;
    }

    // ================= FRIEND RELATIONS =================

    private String[] orderedPair(UUID a, UUID b) {
        String sa = a.toString();
        String sb = b.toString();
        return sa.compareTo(sb) < 0 ? new String[]{sa, sb} : new String[]{sb, sa};
    }

    public boolean areFriends(UUID a, UUID b) throws SQLException {
        String[] pair = orderedPair(a, b);
        String sql = "SELECT id FROM friends_relations WHERE uuid_a = ? AND uuid_b = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pair[0]);
            ps.setString(2, pair[1]);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void addFriendship(UUID a, UUID b) throws SQLException {
        String[] pair = orderedPair(a, b);
        String sql = "INSERT IGNORE INTO friends_relations (uuid_a, uuid_b, added_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pair[0]);
            ps.setString(2, pair[1]);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void removeFriendship(UUID a, UUID b) throws SQLException {
        String[] pair = orderedPair(a, b);
        String sql = "DELETE FROM friends_relations WHERE uuid_a = ? AND uuid_b = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pair[0]);
            ps.setString(2, pair[1]);
            ps.executeUpdate();
        }
    }

    public int getFriendCount(UUID uuid) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM friends_relations WHERE uuid_a = ? OR uuid_b = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        }
        return 0;
    }

    public List<FriendEntry> getFriends(UUID uuid) throws SQLException {
        String sql = """
            SELECT r.uuid_a, r.uuid_b, r.added_at,
                   s.uuid AS f_uuid, s.username AS f_username, s.is_online AS f_online,
                   s.current_server AS f_server, s.status AS f_status, s.last_seen AS f_last_seen
            FROM friends_relations r
            JOIN friends_settings s
                ON s.uuid = CASE WHEN r.uuid_a = ? THEN r.uuid_b ELSE r.uuid_a END
            WHERE r.uuid_a = ? OR r.uuid_b = ?
        """;
        List<FriendEntry> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, uuid.toString());
            ps.setString(3, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID friendUuid = UUID.fromString(rs.getString("f_uuid"));
                    String friendName = rs.getString("f_username");
                    long addedAt = rs.getLong("added_at");
                    boolean online = rs.getBoolean("f_online");
                    String server = rs.getString("f_server");
                    FriendStatus status = FriendStatus.valueOf(rs.getString("f_status"));
                    long lastSeen = rs.getLong("f_last_seen");
                    result.add(new FriendEntry(friendUuid, friendName, addedAt, lastSeen, online, server, status));
                }
            }
        }
        return result;
    }

    // ================= FRIEND REQUESTS =================

    public boolean requestExists(UUID sender, UUID receiver) throws SQLException {
        String sql = "SELECT id FROM friends_requests WHERE sender_uuid = ? AND receiver_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender.toString());
            ps.setString(2, receiver.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void createRequest(UUID sender, UUID receiver) throws SQLException {
        String sql = "INSERT IGNORE INTO friends_requests (sender_uuid, receiver_uuid, created_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender.toString());
            ps.setString(2, receiver.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        }
    }

    public void deleteRequest(UUID sender, UUID receiver) throws SQLException {
        String sql = "DELETE FROM friends_requests WHERE sender_uuid = ? AND receiver_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sender.toString());
            ps.setString(2, receiver.toString());
            ps.executeUpdate();
        }
    }

    public List<FriendRequest> getIncomingRequests(UUID receiver) throws SQLException {
        String sql = "SELECT sender_uuid, receiver_uuid, created_at FROM friends_requests WHERE receiver_uuid = ? ORDER BY created_at DESC";
        List<FriendRequest> result = new ArrayList<>();
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiver.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new FriendRequest(
                            UUID.fromString(rs.getString("sender_uuid")),
                            UUID.fromString(rs.getString("receiver_uuid")),
                            rs.getLong("created_at")
                    ));
                }
            }
        }
        return result;
    }

    public int getIncomingRequestCount(UUID receiver) throws SQLException {
        String sql = "SELECT COUNT(*) AS cnt FROM friends_requests WHERE receiver_uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiver.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("cnt");
            }
        }
        return 0;
    }

    // ================= USERNAME / UUID LOOKUP =================

    public UUID getUuidByUsername(String username) throws SQLException {
        String sql = "SELECT uuid FROM friends_settings WHERE username = ? COLLATE utf8mb4_general_ci";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return UUID.fromString(rs.getString("uuid"));
            }
        }
        return null;
    }

    public String getUsernameByUuid(UUID uuid) throws SQLException {
        String sql = "SELECT username FROM friends_settings WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("username");
            }
        }
        return null;
    }

    // ================= SORT PREFERENCES =================

    public String[] getSortPrefs(UUID uuid) throws SQLException {
        String sql = "SELECT sort_mode, sort_order FROM friends_sort_prefs WHERE uuid = ?";
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new String[]{rs.getString("sort_mode"), rs.getString("sort_order")};
                }
            }
        }
        return new String[]{"DEFAULT", "NORMAL"};
    }

    public void setSortPrefs(UUID uuid, String mode, String order) throws SQLException {
        String sql = """
            INSERT INTO friends_sort_prefs (uuid, sort_mode, sort_order) VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE sort_mode = VALUES(sort_mode), sort_order = VALUES(sort_order)
        """;
        try (Connection conn = db.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, mode);
            ps.setString(3, order);
            ps.executeUpdate();
        }
    }
}
