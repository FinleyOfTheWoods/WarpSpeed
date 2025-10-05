package uk.co.finleyofthewoods.warpspeed.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_FILE = "config/warpspeed/warp_points.db";
    private Connection connection;

    public void initialise() {
        try {
            File dbFile = new File(DB_FILE);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                LOGGER.info("Created database directory {}", parentDir.getAbsolutePath());
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            LOGGER.info("Connected to SQLite database: {}", DB_FILE);

            createTables();
        } catch (SecurityException e) {
            LOGGER.error("Failed to create database directory", e);
        } catch (SQLException e) {
            LOGGER.error("Failed to initialise database", e);
        }
    }

    private void createTables() throws SQLException {
        String createHomesTableSQL = """
                    CREATE TABLE IF NOT EXISTS homes (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        player_uuid TEXT NOT NULL,
                        home_name TEXT NOT NULL,
                        world_id TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        UNIQUE(player_uuid, home_name)
                    );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createHomesTableSQL);
            LOGGER.info("Homes tables created or already exists");
        }

        // Create an index for faster lookups
        String createHomesIndexSQL = "CREATE INDEX IF NOT EXISTS idx_player_homes ON homes(player_uuid);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createHomesIndexSQL);
            LOGGER.info("Homes index created or already exists");
        }

        String createWarpsTableSQL = """
                CREATE TABLE IF NOT EXISTS warps (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  player_uuid TEXT NOT NULL,
                  warp_name TEXT NOT NULL,
                  world_id TEXT NOT NULL,
                  x INTEGER NOT NULL,
                  y INTEGER NOT NULL,
                  z INTEGER NOT NULL,
                  is_private INTEGER NOT NULL,
                  created_at INTEGER NOT NULL,
                  UNIQUE(warp_name)
                );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createWarpsTableSQL);
            LOGGER.info("Warps tables created or already exists");
        }
        String createWarpsIndexSQL = "CREATE INDEX IF NOT EXISTS idx_player_warps ON warps(player_uuid);";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createWarpsIndexSQL);
            LOGGER.info("Warps index created or already exists");
        }
    }
    /**
     * Saves a home position to the database.
     * If a home with the same name exists, it will be updated.
     */
    public boolean saveHome(HomePosition home) {
        String sql = """
            INSERT INTO homes (player_uuid, home_name, world_id, x, y, z, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_uuid, home_name)
            DO UPDATE SET
                world_id = excluded.world_id,
                x = excluded.x,
                y = excluded.y,
                z = excluded.z,
                created_at = excluded.created_at
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, home.getPlayerUUID().toString());
            pstmt.setString(2, home.getHomeName());
            pstmt.setString(3, home.getWorldId());
            pstmt.setInt(4, home.getX());
            pstmt.setInt(5, home.getY());
            pstmt.setInt(6, home.getZ());
            pstmt.setLong(7, home.getCreatedAt());

            int affected = pstmt.executeUpdate();
            LOGGER.debug("Saved home '{}' for player {}", home.getHomeName(), home.getPlayerUUID());
            return affected > 0;

        } catch (SQLException e) {
            LOGGER.error("Failed to save home", e);
            return false;
        }
    }

    /**
     * Retrieves a specific home position for a player.
     */
    public HomePosition getHome(UUID playerUUID, String homeName) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? AND home_name = ? LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, homeName);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new HomePosition(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("home_name"),
                        rs.getString("world_id"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getLong("created_at")
                );
            } else {
                LOGGER.debug("Home '{}' not found", homeName);
                return null;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get home", e);
            return null;
        }
    }

    /**
     * Retrieves all homes for a specific player.
     */
    public List<HomePosition> getPlayerHomes(UUID playerUUID) {
        List<HomePosition> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE player_uuid = ? ORDER BY home_name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                homes.add(new HomePosition(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("home_name"),
                        rs.getString("world_id"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getLong("created_at")
                ));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get player homes", e);
            return homes;
        }
        if (homes.isEmpty()) {
            LOGGER.debug("No homes found for player {}", playerUUID);
        }
        return homes;
    }

    /**
     * Removes a home from the database.
     */
    public boolean removeHome(UUID playerUUID, String homeName) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ? LIMIT 1";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, homeName);

            int affected = pstmt.executeUpdate();
            LOGGER.debug("Removed home '{}' for player {} (affected rows: {})", homeName, playerUUID, affected);
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to remove home", e);
            return false;
        }
    }

    /**
     * Checks if a home exists for a player.
     */
    public boolean homeExists(UUID playerUUID, String homeName) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ? AND home_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, homeName);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            } else {
                LOGGER.debug("Home '{}' not found", homeName);
                return false;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets the count of homes for a player.
     */
    public int getHomeCount(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                LOGGER.debug("No homes found for player {}", playerUUID);
                return 0;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get home count", e);
            return 0;
        }
    }

    public boolean saveWarp(WarpPosition warp) {
        String sql = "INSERT INTO warps (player_uuid, warp_name, world_id, x, y, z, is_private, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warp.getPlayerUUID().toString());
            pstmt.setString(2, warp.getWarpName());
            pstmt.setString(3, warp.getWorldId());
            pstmt.setInt(4, warp.getX());
            pstmt.setInt(5, warp.getY());
            pstmt.setInt(6, warp.getZ());
            pstmt.setBoolean(7, warp.isPrivate());
            pstmt.setLong(8, warp.getCreatedAt());

            int affected = pstmt.executeUpdate();
            LOGGER.debug("Saved warp '{}' for player {}", warp.getWarpName(), warp.getPlayerUUID());
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to save warp", e);
            return false;
        }
    }

    public boolean warpExists(String warpName) {
        String sql = "SELECT count(*) FROM warps WHERE warp_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
             pstmt.setString(1, warpName);

             ResultSet rs = pstmt.executeQuery();
             if (rs.next()) {
                 return rs.getInt(1) > 0;
             } else {
                 LOGGER.debug("Warp '{}' not found", warpName);
                 return false;
             }
        } catch (SQLException e) {
            LOGGER.error("Failed to check if warp exists", e);
            return false;
        }
    }

    public int getWarpCount(UUID playerUUUID) {
        String sql = "SELECT count(*) FROM warps WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get warp count", e);
        }
        return 0;
    }

    public WarpPosition getWarp(String warpName) {
        String sql = "SELECT * FROM warps WHERE warp_name = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, warpName);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new WarpPosition(
                        UUID.fromString(rs.getString("player_uuid")),
                        rs.getString("warp_name"),
                        rs.getString("world_id"),
                        rs.getBoolean("is_private"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getLong("created_at")
                );
            } else {
                LOGGER.debug("Warp '{}' not found", warpName);
                return null;
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get warp", e);
            return null;
        }
    }

    public boolean removeWarp(UUID playerUuid, String warpName) {
        String sql = "DELETE FROM warps WHERE player_uuid = ? AND warp_name = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, warpName);

            int affected = pstmt.executeUpdate();
            LOGGER.debug("Removed warp '{}' for player {} (affected rows: {})", warpName, playerUuid, affected);
            return affected > 0;
        } catch (SQLException e) {
            LOGGER.error("Failed to remove warp", e);
            return false;
        }
    }

    /**
     * Gets all warp names accessible by a player (public warps + their private warps).
     */
    public List<String> getAccessibleWarpNames(UUID playerUUID) {
        List<String> warpNames = new ArrayList<>();
        String sql = "SELECT warp_name FROM warps WHERE is_private = 0 OR player_uuid = ? ORDER BY warp_name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                warpNames.add(rs.getString("warp_name"));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get accessible warp names", e);
        }

        return warpNames;
    }

    /**
     * Gets all warp names owned by a player (for deletion autocomplete).
     */
    public List<String> getPlayerWarpNames(UUID playerUUID) {
        List<String> warpNames = new ArrayList<>();
        String sql = "SELECT warp_name FROM warps WHERE player_uuid = ? ORDER BY warp_name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                warpNames.add(rs.getString("warp_name"));
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to get player warp names", e);
        }

        return warpNames;
    }

    /**
     * Closes the database connection.
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to close database connection", e);
        }
    }
}
