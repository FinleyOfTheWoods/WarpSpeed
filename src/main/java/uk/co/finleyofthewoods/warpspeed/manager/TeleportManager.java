package uk.co.finleyofthewoods.warpspeed.manager;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public interface TeleportManager {
    boolean teleportHome(@NonNull ServerPlayer player, @NonNull String name);
}
