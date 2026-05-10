package uk.co.finleyofthewoods.warpspeed.manager.impl;

import lombok.extern.slf4j.Slf4j;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;
import uk.co.finleyofthewoods.warpspeed.model.WarpLocation;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
public class DatabaseManagerImpl {
    private final File DB_FILE = FabricLoader.getInstance().getConfigDir().resolve("warpspeed/warp_points.db").toFile();

    // SQL statements for home locations
    private static final String INSERT_HOME_LOCATION_SQL = "INSERT INTO homes (player_uuid, home_name, world_id, x, y, z, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String GET_HOME_LOCATION_SQL = "SELECT * FROM homes WHERE player_uuid = ? AND home_name = ? LIMIT 1";
    private static final String GET_ALL_HOME_LOCATIONS_SQL = "SELECT * FROM homes WHERE player_uuid = ? ORDER BY home_name";
    private static final String DELETE_HOME_LOCATION_SQL = "DELETE FROM homes WHERE player_uuid = ? AND home_name = ? LIMIT 1";
    // SQL statements for warp locations
    private static final String INSERT_WARP_LOCATION_SQL = "INSERT INTO warps (player_uuid, warp_name, world_id, x, y, z, is_private, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String GET_WARP_LOCATION_SQL = "SELECT count(*) FROM warps WHERE warp_name = ? LIMIT 1";
    private static final String GET_ALL_WARP_LOCATIONS_SQL = "SELECT warp_name FROM warps WHERE is_private = 0 OR player_uuid = ? ORDER BY warp_name";
    private static final String GET_PLAYER_OWNED_LOCATIONS = "SELECT warp_name FROM warps WHERE player_uuid = ? ORDER BY warp_name";
    private static final String DELETE_WARP_LOCATION_SQL = "DELETE FROM warps WHERE warp_name = ? AND player_uuid = ? LIMIT 1";
    // SQL statements for creating the tables
    private static final String CREATE_HOME_LOCATIONS_TABLE_SQL = "CREATE TABLE IF NOT EXISTS homes (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, home_name TEXT NOT NULL, world_id TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, created_at INTEGER NOT NULL, UNIQUE(player_uuid, home_name));";
    private static final String HOME_LOCATIONS_INDEX_SQL = "CREATE INDEX IF NOT EXISTS idx_player_homes ON homes(player_uuid);";
    private static final String CREATE_WARP_LOCATION_SQL = "CREATE TABLE IF NOT EXISTS warps (id INTEGER PRIMARY KEY AUTOINCREMENT, player_uuid TEXT NOT NULL, warp_name TEXT NOT NULL, world_id TEXT NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, z INTEGER NOT NULL, is_private INTEGER NOT NULL, created_at INTEGER NOT NULL, UNIQUE(warp_name));";
    private static final String WARP_LOCATIONS_INDEX_SQL = "CREATE INDEX IF NOT EXISTS idx_player_warps ON warps(player_uuid);";
    private static final String BLOCK_LIST_SQL = "CREATE TABLE IF NOT EXISTS blocklist (id INTEGER PRIMARY KEY AUTOINCREMENT, blocker_player_username TEXT NOT NULL, blocked_player_username TEXT NOT NULL, created_at INTEGER NOT NULL, UNIQUE(blocker_player_username, blocked_player_username));";
    private static final String BLOCK_LIST_INDEX_SQL = "CREATE INDEX IF NOT EXISTS idx_blocklist_player ON blocklist(blocker_player_username);";

    private Connection connect() {
        log.debug("connecting to database");
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + DB_FILE);

            // Reconfigure connection
            Statement stmt = connection.createStatement();
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");
            stmt.close();

            return connection;
        } catch (Exception e) {
            log.error("Failed to connect to database", e);
            return null;
        }
    }

    public void initialise() throws SQLException {
        log.debug("initialising database");
        File parentDir = DB_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            log.info("database directory does not exist, creating");
            boolean created = parentDir.mkdirs();
            if (!created) {
                log.error("Failed to create database directory");
                throw new SQLException("Failed to create database directory");
            }
        }
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }

            log.info("initialising database tables");
            log.debug("creating home locations table");
            PreparedStatement stmt = connection.prepareStatement(CREATE_HOME_LOCATIONS_TABLE_SQL);
            stmt.executeUpdate();
            log.debug("creating home locations index");
            stmt = connection.prepareStatement(HOME_LOCATIONS_INDEX_SQL);
            stmt.executeUpdate();
            log.debug("creating warps table");
            stmt = connection.prepareStatement(CREATE_WARP_LOCATION_SQL);
            stmt.executeUpdate();
            log.debug("creating warps index");
            stmt = connection.prepareStatement(WARP_LOCATIONS_INDEX_SQL);
            stmt.executeUpdate();
            log.debug("creating blocklist table");
            stmt = connection.prepareStatement(BLOCK_LIST_SQL);
            stmt.executeUpdate();
            log.debug("creating blocklist index");
            stmt = connection.prepareStatement(BLOCK_LIST_INDEX_SQL);
            stmt.executeUpdate();
            log.info("database initialised");
        } catch (Exception e) {
            throw new SQLException("Failed to initialise database");
        }
    }

    public boolean insertHomeLocation(HomeLocation homeLocation) {
        log.debug("inserting home location {}", homeLocation);
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(INSERT_HOME_LOCATION_SQL);
            stmt.setString(1, homeLocation.getUuid().toString());
            stmt.setString(2, homeLocation.getName());
            stmt.setString(3, homeLocation.getLevel().dimension().identifier().toString());
            stmt.setInt(4, homeLocation.getPos().getX());
            stmt.setInt(5, homeLocation.getPos().getY());
            stmt.setInt(6, homeLocation.getPos().getZ());
            stmt.setLong(7, System.currentTimeMillis());
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error("Failed to insert home location");
            return false;
        }
    }

    public @Nullable HomeLocation getHomeLocation(@NonNull ServerPlayer player, @NonNull String homeName) {
        log.debug("getting home location named {} for {}", homeName, player.getPlainTextName());
        try (Connection connection = connect()) {
            if (connection == null) {
                log.error("Failed to connect to database");
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(GET_HOME_LOCATION_SQL);
            stmt.setString(1, player.getStringUUID());
            stmt.setString(2, homeName);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ServerLevel serverLevel = player.level();
                return HomeLocation.getFromResultSet(rs, serverLevel);
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get home location", e);
            return null;
        }
    }

    public List<HomeLocation> getAllHomeLocations(ServerPlayer player) {
        log.debug("getting all home locations for {}", player.getPlainTextName());
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(GET_ALL_HOME_LOCATIONS_SQL);
            stmt.setString(1, player.getStringUUID());
            ResultSet rs = stmt.executeQuery();
            List<HomeLocation> homeLocations = new ArrayList<>();
            while (rs.next()) {
                ServerLevel serverLevel = player.level();
                HomeLocation home = HomeLocation.getFromResultSet(rs, serverLevel);
                homeLocations.add(home);
            }
            if (homeLocations.isEmpty()) return null;
            return homeLocations;
        } catch (Exception e) {
            log.error("Failed to get home locations for player: {} - {}", player.getPlainTextName(), e.getMessage());
            return null;
        }
    }

    public boolean deleteHomeLocation(ServerPlayer player, String homeName) {
        log.debug("deleting home location named {} for {}", homeName, player.getPlainTextName());
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(DELETE_HOME_LOCATION_SQL);
            stmt.setString(1, player.getStringUUID());
            stmt.setString(2, homeName);
            int deleted = stmt.executeUpdate();
            if (deleted == 0) {
                log.debug("Failed to delete home location: no record found for player {} and home {}", player.getPlainTextName(), homeName);
                return false;
            }
            log.debug("Deleted home location for player {} and home {}", player.getPlainTextName(), homeName);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete home location", e);
            return false;
        }
    }

    public boolean insertWarpLocation(WarpLocation location) {
        log.debug("inserting warp location {} at {}", location.getName(), location.getPos());
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(INSERT_WARP_LOCATION_SQL);
            stmt.setString(1, location.getUuid().toString());
            stmt.setString(2, location.getName());
            stmt.setString(3, location.getLevel().dimension().registry().toString());
            stmt.setInt(4, location.getPos().getX());
            stmt.setInt(5, location.getPos().getY());
            stmt.setInt(6, location.getPos().getZ());
            stmt.setInt(7, location.isPrivate() ? 1 : 0);
            return true;
        } catch (Exception e) {
            log.error("Failed to insert warp location", e);
            return false;
        }
    }

    public WarpLocation getWarpLocation(ServerPlayer player, String homeName) {
        log.debug("getting warp location named {}", homeName);
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(GET_WARP_LOCATION_SQL);
            stmt.setString(1, homeName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ServerLevel serverLevel = player.level();
                MinecraftServer server = serverLevel.getServer();
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String warpName = rs.getString("warp_name");
                String level = rs.getString("world_id");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                boolean isPrivate = rs.getBoolean("is_private");
                return new WarpLocation(uuid, warpName, isPrivate, new BlockPos(x, y, z), getServerLevel(server, level));
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get warp location", e);
            return null;
        }
    }

    public boolean deleteWarpLocation(ServerPlayer player, String homeName) {
        log.debug("deleting warp location named {}", homeName);
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(DELETE_WARP_LOCATION_SQL);
            stmt.setString(1, homeName);
            stmt.setString(2, player.getStringUUID());
            stmt.executeUpdate();
            return true;
        } catch (Exception e) {
            log.error("Failed to delete warp location", e);
            return false;
        }
    }

    public List<WarpLocation> getAllWarpLocations(ServerPlayer player) {
        log.debug("getting all warp locations available for {}", player.getPlainTextName());
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(GET_ALL_WARP_LOCATIONS_SQL);
            stmt.setString(1, player.getStringUUID());
            ResultSet rs = stmt.executeQuery();
            List<WarpLocation> warpLocations = new ArrayList<>();
            while (rs.next()) {
                ServerLevel serverLevel = player.level();
                MinecraftServer server = serverLevel.getServer();
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String warpName = rs.getString("warp_name");
                String level = rs.getString("world_id");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                boolean isPrivate = rs.getBoolean("is_private");
                warpLocations.add(new WarpLocation(uuid, warpName, isPrivate, new BlockPos(x, y, z), getServerLevel(server, level)));

            }
            return warpLocations;
        } catch (Exception e) {
            log.error("Failed to get warp locations", e);
            return null;
        }
    }

    public List<WarpLocation> getPlayerOwnedLocations(ServerPlayer player) {
        log.debug("getting all warp locations owned by {}", player.getPlainTextName());
        try (Connection connection = connect()) {
            if (connection == null) {
                throw new SQLException("Failed to connect to database");
            }
            PreparedStatement stmt = connection.prepareStatement(GET_PLAYER_OWNED_LOCATIONS);
            stmt.setString(1, player.getStringUUID());
            ResultSet rs = stmt.executeQuery();
            List<WarpLocation> warpLocations = new ArrayList<>();
            while (rs.next()) {
                ServerLevel serverLevel = player.level();
                MinecraftServer server = serverLevel.getServer();
                UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                String warpName = rs.getString("warp_name");
                String level = rs.getString("world_id");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                boolean isPrivate = rs.getBoolean("is_private");
                warpLocations.add(new WarpLocation(uuid, warpName, isPrivate, new BlockPos(x, y, z), getServerLevel(server, level)));

            }
            return warpLocations;
        } catch (Exception e) {
            log.error("Failed to get warp locations", e);
            return null;
        }
    }

    private ServerLevel getServerLevel(MinecraftServer server, String level) {
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(level));
        return server.getLevel(levelKey);
    }
}
