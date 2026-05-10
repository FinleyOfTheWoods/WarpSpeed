package uk.co.finleyofthewoods.warpspeed.manager.impl;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;

import java.util.List;

public class LocationManagerImpl {
    private static final DatabaseManagerImpl databaseManager = new DatabaseManagerImpl();

    public @Nullable HomeLocation getHomeLocationByName(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.getHomeLocation(player, name);
    }

    public @Nullable List<HomeLocation> getHomeLocationsByPlayerId(@NonNull ServerPlayer player) {
        return databaseManager.getAllHomeLocations(player);
    }

    public boolean createHomeLocation(@NonNull ServerPlayer player, @NonNull String name) {
        HomeLocation home = new HomeLocation(player.getUUID(), name, player.getOnPos(), player.level());
        return databaseManager.insertHomeLocation(home);
    }

    public boolean deleteHomeLocation(@NonNull ServerPlayer player, @NonNull String name) {
        return databaseManager.deleteHomeLocation(player, name);
    }
}
