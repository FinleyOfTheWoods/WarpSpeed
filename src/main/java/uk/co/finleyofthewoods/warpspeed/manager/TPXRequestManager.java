package uk.co.finleyofthewoods.warpspeed.manager;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.TPARequest;

import java.util.List;

public interface TPXRequestManager {
    void createRequest(@NonNull TPARequest request);

    @Nullable TPARequest getRequest(@NonNull ServerPlayer player, @NonNull ServerPlayer target);

    @Nullable List<TPARequest> getRequests(@NonNull ServerPlayer player);

    void removeRequest(@NonNull ServerPlayer player);

    void clearExpiredRequests();
}
