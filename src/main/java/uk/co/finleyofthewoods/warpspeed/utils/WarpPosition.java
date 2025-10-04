package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class WarpPosition {
    private final UUID playerUUID;
    private final String warpName;
    private final String worldId;
    private final boolean isPrivate;
    private final int x;
    private final int y;
    private final int z;
    private final long createdAt;

    public WarpPosition(UUID playerUUID, String warpName, String worldId, boolean isPrivate, int x, int y, int z, long createdAt) {
        this.playerUUID = playerUUID;
        this.warpName = warpName;
        this.worldId = worldId;
        this.isPrivate = isPrivate;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = createdAt;
    }

    public WarpPosition(UUID playerUUID, String warpName, String worldId, boolean isPrivate, int x, int y, int z) {
        this.playerUUID = playerUUID;
        this.warpName = warpName;
        this.worldId = worldId;
        this.isPrivate = isPrivate;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getWarpName() {
        return warpName;
    }

    public String getWorldId() {
        return worldId;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public long getCreatedAt() {
        return createdAt;
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
