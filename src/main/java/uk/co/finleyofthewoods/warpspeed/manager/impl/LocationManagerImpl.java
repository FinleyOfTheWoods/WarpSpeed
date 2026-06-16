package uk.co.finleyofthewoods.warpspeed.manager.impl;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayer.RespawnConfig;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.BaseLocation;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;
import uk.co.finleyofthewoods.warpspeed.model.WarpLocation;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LocationManagerImpl implements uk.co.finleyofthewoods.warpspeed.manager.LocationManager {
    private static final DatabaseManagerImpl databaseManager = new DatabaseManagerImpl();

    private static final ConcurrentHashMap<UUID, BaseLocation> previousLocations = new ConcurrentHashMap<>();

    @Override
    public @Nullable HomeLocation getBedLocation(@NonNull ServerPlayer player) {
        RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig == null) {
            log.debug("Respawn config is null for {}", player.getPlainTextName());
            return null;
        }
        BlockPos pos = respawnConfig.respawnData().pos().above();
        ResourceKey<Level> dimension = respawnConfig.respawnData().dimension();
        MinecraftServer server = player.level().getServer();
        ServerLevel bedLevel = server.getLevel(dimension);
        log.debug("{} respawn location: {} {}", player.getPlainTextName(), dimension, pos);
        return new HomeLocation(player.getUUID(), "bed", pos, bedLevel);
    }

    @Override
    public @Nullable HomeLocation getHomeLocationByName(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.getHomeLocationByName(player, name);
    }

    @Override
    public @Nullable List<HomeLocation> getHomeLocationsByPlayerId(@NonNull ServerPlayer player) {
        return databaseManager.getHomeLocationsByPlayerId(player);
    }

    @Override
    public boolean insertHomeLocation(@NonNull ServerPlayer player, @NonNull String name) {
        HomeLocation home = new HomeLocation(player.getUUID(), name, player.getOnPos().above(), player.level());
        return databaseManager.insertHomeLocation(home);
    }

    @Override
    public boolean deleteHomeLocation(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.deleteHomeLocation(player, name);
    }

    @Override
    public @Nullable WarpLocation getWarpLocationByName(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.getWarpLocation(player, name);
    }

    public @Nullable List<WarpLocation> getAllWarpLocations(@NonNull ServerPlayer player) {
        return databaseManager.getAllWarpLocations(player);
    }

    @Override
    public @Nullable List<WarpLocation> getPlayerOwnedLocations(@NonNull ServerPlayer player) {
        return databaseManager.getPlayerOwnedLocations(player);
    }

    @Override
    public boolean insertWarpLocation(@NonNull ServerPlayer player, @NonNull String name, boolean isPrivate) {
        WarpLocation location = new WarpLocation(player.getUUID(), name, isPrivate, player.getOnPos().above(), player.level());
        return databaseManager.insertWarpLocation(location);
    }

    @Override
    public boolean deleteWarpLocation(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.deleteWarpLocation(player, name);
    }

    @Override
    public void setPreviousLocation(@NonNull ServerPlayer player, @NonNull BlockPos pos) {
        log.debug("setting previous location for {} at {}", player.getPlainTextName(), pos);
        ServerLevel level = player.level();
        BaseLocation previousLocation = new BaseLocation(player.getUUID(), pos, level);
        if (previousLocations.containsKey(player.getUUID())) {
            previousLocations.replace(player.getUUID(), previousLocation);
        } else {
            previousLocations.put(player.getUUID(), previousLocation);
        }
    }

    @Override
    public @Nullable BaseLocation getPreviousLocation(@NonNull ServerPlayer player) {
        return previousLocations.get(player.getUUID());
    }

    @Override
    public void clearPreviousLocations(@NonNull ServerPlayer player) {
        previousLocations.remove(player.getUUID());
    }
}
