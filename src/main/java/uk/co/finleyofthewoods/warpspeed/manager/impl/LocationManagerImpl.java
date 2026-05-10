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
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;

import java.util.List;

@Slf4j
public class LocationManagerImpl implements uk.co.finleyofthewoods.warpspeed.manager.LocationManager {
    private static final DatabaseManagerImpl databaseManager = new DatabaseManagerImpl();

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
        return databaseManager.getHomeLocation(player, name);
    }

    @Override
    public @Nullable List<HomeLocation> getHomeLocationsByPlayerId(@NonNull ServerPlayer player) {
        return databaseManager.getAllHomeLocations(player);
    }

    @Override
    public boolean createHomeLocation(@NonNull ServerPlayer player, @NonNull String name) {
        HomeLocation home = new HomeLocation(player.getUUID(), name, player.getOnPos(), player.level());
        return databaseManager.insertHomeLocation(home);
    }

    @Override
    public boolean deleteHomeLocation(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.deleteHomeLocation(player, name);
    }
}
