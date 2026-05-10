package uk.co.finleyofthewoods.warpspeed.manager;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;

import java.util.List;

public interface LocationManager {
    @Nullable HomeLocation getBedLocation(@NonNull ServerPlayer player);

    @Nullable HomeLocation getHomeLocationByName(@NonNull ServerPlayer player, @NonNull String name);

    @Nullable List<HomeLocation> getHomeLocationsByPlayerId(@NonNull ServerPlayer player);

    boolean createHomeLocation(@NonNull ServerPlayer player, @NonNull String name);

    boolean deleteHomeLocation(@NonNull ServerPlayer player, @NonNull String name);
}
