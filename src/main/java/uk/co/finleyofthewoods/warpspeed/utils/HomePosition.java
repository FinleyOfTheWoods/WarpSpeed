package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record HomePosition(UUID playerUUID, String homeName, String worldId, int x, int y, int z, long createdAt) {
    public HomePosition(UUID playerUUID, String homeName, String worldId, int x, int y, int z) {
        this(playerUUID, homeName, worldId, x, y, z, System.currentTimeMillis());
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }

    @Override
    public String toString() {
        return String.format("HomePosition{player=%s, name=%s, world=%s, pos=(%d, %d, %d)}",
                playerUUID, homeName, worldId, x, y, z);
    }

    public String toJson() {
        return String.format("{\"player\": \"%s\", \"name\": \"%s\", \"world\": \"%s\", \"pos\": [%d, %d, %d], \"createdAt\": %d}",
                playerUUID.toString(), homeName, worldId, x, y, z, createdAt);
    }
}
