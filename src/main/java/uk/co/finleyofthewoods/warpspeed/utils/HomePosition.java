package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public class HomePosition {
    private final UUID playerUUID;
    private final String homeName;
    private final String worldId;
    private final int x;
    private final int y;
    private final int z;
    private final long createdAt;

    public HomePosition(UUID playerUUID, String homeName, String worldId, int x, int y, int z) {
        this.playerUUID = playerUUID;
        this.homeName = homeName;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = System.currentTimeMillis();
    }

    public HomePosition(UUID playerUUID, String homeName, String worldId, int x, int y, int z, long createdAt) {
        this.playerUUID = playerUUID;
        this.homeName = homeName;
        this.worldId = worldId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.createdAt = createdAt;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getHomeName() {
        return homeName;
    }

    public String getWorldId() {
        return worldId;
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
        return String.format("HomePosition{player=%s, name=%s, world=%s, pos=(%d, %d, %d)}",
                playerUUID, homeName, worldId, x, y, z);
    }

    public String toJson() {
        return String.format("{\"player\": \"%s\", \"name\": \"%s\", \"world\": \"%s\", \"pos\": [%d, %d, %d], \"createdAt\": %d}",
                playerUUID.toString(), homeName, worldId, x, y, z, createdAt);
    }
}
