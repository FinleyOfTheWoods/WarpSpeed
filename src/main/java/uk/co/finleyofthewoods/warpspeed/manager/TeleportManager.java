package uk.co.finleyofthewoods.warpspeed.manager;

import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;

public interface TeleportManager {
    boolean teleportHome(@NonNull ServerPlayer player, @NonNull String name);
    boolean teleportSpawn(@NonNull ServerPlayer player);
    boolean teleportWarp(@NonNull ServerPlayer player, @NonNull String name);
    boolean teleportRandomly(@NonNull ServerPlayer player);
    boolean teleportBack(@NonNull ServerPlayer player);
}
