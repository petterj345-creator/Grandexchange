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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * SQLite-backed storage for the order book ({@code offers}) and collection box
 * ({@code collection}). All access runs on the main server thread, so a single
 * synchronized connection is sufficient.
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
                    CREATE TABLE IF NOT EXISTS offers (
                      id             INTEGER PRIMARY KEY AUTOINCREMENT,
                      owner_uuid     TEXT    NOT NULL,
                      owner_name     TEXT    NOT NULL,
                      side           TEXT    NOT NULL,
                      item_data      BLOB    NOT NULL,
                      item_label     TEXT    NOT NULL,
                      quantity       INTEGER NOT NULL,
                      price_per_item REAL    NOT NULL,
                      escrow_coins   REAL    NOT NULL DEFAULT 0,
                      created_at     INTEGER NOT NULL
                    )
                    """);
            st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS collection (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      owner_uuid  TEXT    NOT NULL,
                      item_data   BLOB,
                      item_label  TEXT,
                      quantity    INTEGER NOT NULL DEFAULT 0,
                      coins       REAL    NOT NULL DEFAULT 0,
                      created_at  INTEGER NOT NULL
                    )
                    """);
        }
    }

    // ---------------------------------------------------------------- offers

    public synchronized Offer insertOffer(UUID ownerUuid, String ownerName, OfferSide side,
                                          ItemStack single, int quantity, double pricePerItem,
                                          double escrowCoins, long createdAt) throws SQLException {
        String sql = "INSERT INTO offers "
                + "(owner_uuid, owner_name, side, item_data, item_label, quantity, price_per_item, escrow_coins, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, ownerUuid.toString());
            ps.setString(2, ownerName);
            ps.setString(3, side.name());
            ps.setBytes(4, single.serializeAsBytes());
            ps.setString(5, single.getType().name());
            ps.setInt(6, quantity);
            ps.setDouble(7, pricePerItem);
            ps.setDouble(8, escrowCoins);
            ps.setLong(9, createdAt);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                long id = keys.next() ? keys.getLong(1) : -1;
                return new Offer(id, ownerUuid, ownerName, side, single, quantity,
                        pricePerItem, escrowCoins, createdAt);
            }
        }
    }

    public synchronized Offer offerById(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM offers WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? readOffer(rs) : null;
            }
        }
    }

    public synchronized List<Offer> offersByOwner(UUID owner) throws SQLException {
        List<Offer> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM offers WHERE owner_uuid = ? AND quantity > 0 ORDER BY created_at ASC")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readOffer(rs));
                }
            }
        }
        return out;
    }

    /** Resting SELL offers for an item priced at or below {@code maxPrice}, cheapest (then oldest) first. */
    public synchronized List<Offer> matchableSells(String label, double maxPrice) throws SQLException {
        List<Offer> out = new ArrayList<>();
        String sql = "SELECT * FROM offers WHERE side = 'SELL' AND item_label = ? AND quantity > 0 "
                + "AND price_per_item <= ? ORDER BY price_per_item ASC, created_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, label);
            ps.setDouble(2, maxPrice);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readOffer(rs));
                }
            }
        }
        return out;
    }

    /** Resting BUY offers for an item priced at or above {@code minPrice}, highest (then oldest) first. */
    public synchronized List<Offer> matchableBuys(String label, double minPrice) throws SQLException {
        List<Offer> out = new ArrayList<>();
        String sql = "SELECT * FROM offers WHERE side = 'BUY' AND item_label = ? AND quantity > 0 "
                + "AND price_per_item >= ? ORDER BY price_per_item DESC, created_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, label);
            ps.setDouble(2, minPrice);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(readOffer(rs));
                }
            }
        }
        return out;
    }

    public synchronized void updateOffer(long id, int quantity, double escrowCoins) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE offers SET quantity = ?, escrow_coins = ? WHERE id = ?")) {
            ps.setInt(1, quantity);
            ps.setDouble(2, escrowCoins);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }

    public synchronized void deleteOffer(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM offers WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Aggregated best ask / best bid for every item that has at least one resting offer. */
    public synchronized List<MarketSummary> marketSummaries() throws SQLException {
        Map<String, MarketSummary> byLabel = new LinkedHashMap<>();
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM offers WHERE quantity > 0 ORDER BY item_label ASC")) {
            while (rs.next()) {
                Offer offer = readOffer(rs);
                byLabel.merge(offer.label(), summaryFromSingle(offer), Database::mergeSummary);
            }
        }
        return new ArrayList<>(byLabel.values());
    }

    public synchronized MarketSummary marketSummary(String label) throws SQLException {
        MarketSummary summary = null;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM offers WHERE item_label = ? AND quantity > 0")) {
            ps.setString(1, label);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Offer offer = readOffer(rs);
                    summary = summary == null
                            ? summaryFromSingle(offer)
                            : mergeSummary(summary, summaryFromSingle(offer));
                }
            }
        }
        return summary;
    }

    private static MarketSummary summaryFromSingle(Offer offer) {
        if (offer.side() == OfferSide.SELL) {
            return new MarketSummary(offer.label(), offer.item(),
                    offer.pricePerItem(), offer.quantity(), 0, 0);
        }
        return new MarketSummary(offer.label(), offer.item(),
                0, 0, offer.pricePerItem(), offer.quantity());
    }

    private static MarketSummary mergeSummary(MarketSummary a, MarketSummary b) {
        double lowestAsk;
        if (a.askQuantity() == 0) {
            lowestAsk = b.lowestAsk();
        } else if (b.askQuantity() == 0) {
            lowestAsk = a.lowestAsk();
        } else {
            lowestAsk = Math.min(a.lowestAsk(), b.lowestAsk());
        }
        double highestBid = Math.max(a.highestBid(), b.highestBid());
        ItemStack item = a.item() != null ? a.item() : b.item();
        return new MarketSummary(a.label(), item,
                lowestAsk, a.askQuantity() + b.askQuantity(),
                highestBid, a.bidQuantity() + b.bidQuantity());
    }

    private Offer readOffer(ResultSet rs) throws SQLException {
        ItemStack item = ItemStack.deserializeBytes(rs.getBytes("item_data"));
        return new Offer(
                rs.getLong("id"),
                UUID.fromString(rs.getString("owner_uuid")),
                rs.getString("owner_name"),
                OfferSide.valueOf(rs.getString("side")),
                item,
                rs.getInt("quantity"),
                rs.getDouble("price_per_item"),
                rs.getDouble("escrow_coins"),
                rs.getLong("created_at"));
    }

    // ------------------------------------------------------------ collection

    public synchronized void addCollectionItem(UUID owner, ItemStack single, int quantity,
                                               long createdAt) throws SQLException {
        String sql = "INSERT INTO collection (owner_uuid, item_data, item_label, quantity, coins, created_at) "
                + "VALUES (?, ?, ?, ?, 0, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setBytes(2, single.serializeAsBytes());
            ps.setString(3, single.getType().name());
            ps.setInt(4, quantity);
            ps.setLong(5, createdAt);
            ps.executeUpdate();
        }
    }

    public synchronized void addCollectionCoins(UUID owner, double coins, long createdAt) throws SQLException {
        if (coins <= 0) {
            return;
        }
        String sql = "INSERT INTO collection (owner_uuid, item_data, item_label, quantity, coins, created_at) "
                + "VALUES (?, NULL, NULL, 0, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, owner.toString());
            ps.setDouble(2, coins);
            ps.setLong(3, createdAt);
            ps.executeUpdate();
        }
    }

    public synchronized List<CollectionEntry> collectionByOwner(UUID owner) throws SQLException {
        List<CollectionEntry> out = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM collection WHERE owner_uuid = ? ORDER BY created_at ASC")) {
            ps.setString(1, owner.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byte[] data = rs.getBytes("item_data");
                    ItemStack item = data == null ? null : ItemStack.deserializeBytes(data);
                    out.add(new CollectionEntry(
                            rs.getLong("id"),
                            UUID.fromString(rs.getString("owner_uuid")),
                            item,
                            rs.getInt("quantity"),
                            rs.getDouble("coins"),
                            rs.getLong("created_at")));
                }
            }
        }
        return out;
    }

    public synchronized void deleteCollection(long id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM collection WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
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
