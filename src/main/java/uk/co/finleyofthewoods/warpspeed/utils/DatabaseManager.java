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
    private static final String DB_FILE = "config/warpspeed/homes.db";
    private Connection connection;

    public void initialise() {
        try {
            File dbFile = new File(DB_FILE);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
                LOGGER.info("Created homes database directory {}", parentDir.getAbsolutePath());
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);
            LOGGER.info("Connected to SQLite database: {}", DB_FILE);

            createTables();
        } catch (SecurityException e) {
            LOGGER.error("Failed to create homes database directory", e);
        } catch (SQLException e) {
            LOGGER.error("Failed to initialise homes database", e);
        }
    }

    private void createTables() throws SQLException {
        String createTableSQL = """
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
                    )
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            LOGGER.info("Homes table created or already exists");
        }

        // Create index for faster lookups
        String createIndexSQL = "CREATE INDEX IF NOT EXISTS idx_player_homes ON homes(player_uuid)";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createIndexSQL);
            LOGGER.info("Homes index created or already exists");
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
    public HomePosition getHome(UUID playerUuid, String homeName) {
        String sql = "SELECT * FROM homes WHERE player_uuid = ? AND home_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
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
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to retrieve home", e);
        }

        return null;
    }

    /**
     * Retrieves all homes for a specific player.
     */
    public List<HomePosition> getPlayerHomes(UUID playerUuid) {
        List<HomePosition> homes = new ArrayList<>();
        String sql = "SELECT * FROM homes WHERE player_uuid = ? ORDER BY home_name";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());

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
            LOGGER.error("Failed to retrieve player homes", e);
        }

        return homes;
    }

    /**
     * Removes a home from the database.
     */
    public boolean removeHome(UUID playerUuid, String homeName) {
        String sql = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, homeName);

            int affected = pstmt.executeUpdate();
            LOGGER.debug("Removed home '{}' for player {} (affected rows: {})", homeName, playerUuid, affected);
            return affected > 0;

        } catch (SQLException e) {
            LOGGER.error("Failed to remove home", e);
            return false;
        }
    }

    /**
     * Checks if a home exists for a player.
     */
    public boolean homeExists(UUID playerUuid, String homeName) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ? AND home_name = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, homeName);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to check home existence", e);
        }

        return false;
    }

    /**
     * Gets the count of homes for a player.
     */
    public int getHomeCount(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM homes WHERE player_uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            LOGGER.error("Failed to get home count", e);
        }

        return 0;
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
