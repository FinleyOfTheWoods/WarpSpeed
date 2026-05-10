package uk.co.finleyofthewoods.warpspeed.manager;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;
import uk.co.finleyofthewoods.warpspeed.model.WarpLocation;

import java.util.List;

public interface LocationManager {
    @Nullable HomeLocation getBedLocation(@NonNull ServerPlayer player);

    @Nullable HomeLocation getHomeLocationByName(@NonNull ServerPlayer player, @NonNull String name);

    @Nullable List<HomeLocation> getHomeLocationsByPlayerId(@NonNull ServerPlayer player);

    boolean insertHomeLocation(@NonNull ServerPlayer player, @NonNull String name);

    boolean deleteHomeLocation(@NonNull ServerPlayer player, @NonNull String name);

    @Nullable WarpLocation getWarpLocationByName(@NonNull ServerPlayer player, @NonNull String name);

    @Nullable List<WarpLocation> getPlayerOwnedLocations(@NonNull ServerPlayer player);

    boolean insertWarpLocation(@NonNull ServerPlayer player, @NonNull String name, boolean isPrivate);

    boolean deleteWarpLocation(@NonNull ServerPlayer player, @NonNull String name);
}
