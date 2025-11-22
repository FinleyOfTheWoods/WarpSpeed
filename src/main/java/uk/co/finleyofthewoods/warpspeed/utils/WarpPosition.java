package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record WarpPosition(UUID playerUUID, String warpName, String worldId, boolean isPrivate, int x, int y, int z,
                           long createdAt) {

    public WarpPosition(UUID playerUUID, String warpName, String worldId, boolean isPrivate, int x, int y, int z) {
        this(playerUUID, warpName, worldId, isPrivate, x, y, z, System.currentTimeMillis());
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("WarpPosition{player=%s, name=%s, world=%s, pos=(%d, %d, %d), isPrivate=%s}",
                playerUUID, warpName, worldId, x, y, z, isPrivate);
    }
}
