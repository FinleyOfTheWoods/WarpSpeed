package uk.co.finleyofthewoods.warpspeed.manager;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;
import uk.co.finleyofthewoods.warpspeed.model.WarpLocation;

import java.sql.SQLException;
import java.util.List;

public interface DatabaseManager {
    void initialise() throws SQLException;

    boolean insertHomeLocation(HomeLocation homeLocation);

    @Nullable HomeLocation getHomeLocationByName(@NonNull ServerPlayer player, @NonNull String homeName);

    List<HomeLocation> getHomeLocationsByPlayerId(ServerPlayer player);

    boolean deleteHomeLocation(ServerPlayer player, String homeName);

    boolean insertWarpLocation(WarpLocation location);

    WarpLocation getWarpLocation(ServerPlayer player, String homeName);

    boolean deleteWarpLocation(ServerPlayer player, String homeName);

    List<WarpLocation> getAllWarpLocations(ServerPlayer player);

    List<WarpLocation> getPlayerOwnedLocations(ServerPlayer player);
}
