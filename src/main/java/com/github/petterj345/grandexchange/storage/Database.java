package com.github.petterj345.grandexchange.storage;

import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQLite-backed storage for listings. All access runs on the main server thread
 * (commands, GUI clicks, and synced chat callbacks), so a single connection is fine.
 */
public final class Database {

    private final JavaPlugin plugin;
    private Connection connection;

    public Database(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() throws SQLException {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not available", e);
        }
        File dbFile = new File(plugin.getDataFolder(), "grandexchange.db");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS listings (
                      id             INTEGER PRIMARY KEY AUTOINCREMENT,
                      seller_uuid    TEXT    NOT NULL,
                      seller_name    TEXT    NOT NULL,
                      item_data      BLOB    NOT NULL,
                      item_label     TEXT    NOT NULL,
                      quantity       INTEGER NOT NULL,
                      price_per_item REAL    NOT NULL,
                      created_at     INTEGER NOT NULL
                    )
                    """);
        }
    }

    public synchronized Listing insert(UUID sellerUuid, String sellerName, ItemStack single,
                                       int quantity, double pricePerItem, long createdAt) throws SQLException {
        String sql = "INSERT INTO listings "
                + "(seller_uuid, seller_name, item_data, item_label, quantity, price_per_item, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sellerUuid.toString());
            ps.setString(2, sellerName);
            ps.setBytes(3, single.serializeAsBytes());
            ps.setString(4, single.getType().name());
            ps.setInt(5, quantity);
            ps.setDouble(6, pricePerItem);
            ps.setLong(7, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : -1;
                return new Listing(id, sellerUuid, sellerName, single, quantity, pricePerItem, createdAt);
            }
        }
    }

    public synchronized List<Listing> all() throws SQLException {
        List<Listing> out = new ArrayList<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM listings WHERE quantity > 0 ORDER BY created_at ASC")) {
            while (rs.next()) {
                out.add(read(rs));
            }
        }
        return out;
    }

    public synchronized List<Listing> bySeller(UUID seller) throws SQLException {
        List<Listing> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM listings WHERE seller_uuid = ? AND quantity > 0 ORDER BY created_at ASC")) {
            ps.setString(1, seller.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(read(rs));
                }
            }
        }
        return out;
    }

    public synchronized Listing byId(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM listings WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? read(rs) : null;
            }
        }
    }

    /** Average / minimum / count of active listings for the given item label (material name). */
    public synchronized MarketStats marketStats(String label) throws SQLException {
        String sql = "SELECT COUNT(*) AS c, AVG(price_per_item) AS a, MIN(price_per_item) AS m "
                + "FROM listings WHERE item_label = ? AND quantity > 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, label);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("c");
                    if (count == 0) {
                        return MarketStats.EMPTY;
                    }
                    return new MarketStats(count, rs.getDouble("a"), rs.getDouble("m"));
                }
            }
        }
        return MarketStats.EMPTY;
    }

    public synchronized void updateQuantity(long id, int quantity) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE listings SET quantity = ? WHERE id = ?")) {
            ps.setInt(1, quantity);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    public synchronized void delete(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM listings WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Listing read(ResultSet rs) throws SQLException {
        ItemStack item = ItemStack.deserializeBytes(rs.getBytes("item_data"));
        return new Listing(
                rs.getLong("id"),
                UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("seller_name"),
                item,
                rs.getInt("quantity"),
                rs.getDouble("price_per_item"),
                rs.getLong("created_at"));
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
            // closing on shutdown; nothing to recover
        }
    }
}
