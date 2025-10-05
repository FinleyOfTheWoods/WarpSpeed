package uk.co.finleyofthewoods.warpspeed.utils;


import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerLocationTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerLocationTracker.class);

    private static final Map<UUID, PlayerLocation> previousLocations = new HashMap<>();
    private static final Map<UUID, PlayerLocation> deathLocations = new HashMap<>();

    public static class PlayerLocation {
        private final int x;
        private final int y;
        private final int z;
        private final String worldId;

        public PlayerLocation(int x, int y, int z, String worldId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.worldId = worldId;
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

        public BlockPos getBlockPos() {
            return new BlockPos(x, y, z);
        }

        public String getWorldId() {
            return worldId;
        }
    }

    public static void storeCurrentLocation(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BlockPos pos = player.getBlockPos();
        String worldId = player.getEntityWorld().getRegistryKey().getValue().toString();
        PlayerLocation location = new PlayerLocation(pos.getX(), pos.getY(), pos.getZ(), worldId);
        previousLocations.put(uuid, location);
        LOGGER.debug("Stored location for player {} at ({}, {}, {})", player.getName().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static void storeDeathLocation(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        BlockPos pos = player.getBlockPos();
        String worldId = player.getEntityWorld().getRegistryKey().getValue().toString();
        PlayerLocation location = new PlayerLocation(pos.getX(), pos.getY(), pos.getZ(), worldId);
        deathLocations.put(uuid, location);
        LOGGER.debug("Stored death location for player {} at ({}, {}, {})", player.getName().toString(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static PlayerLocation getPreviousLocation(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        PlayerLocation deathLocation = deathLocations.get(uuid);
        if (deathLocation != null) {
            LOGGER.debug("Returning death location for player {} at ({}, {}, {})", player.getName().toString(), deathLocation.x, deathLocation.y, deathLocation.z);
            deathLocations.remove(uuid);
            return deathLocation;
        }
        return previousLocations.get(uuid);
    }

    public static boolean hasPreviousLocation(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        return previousLocations.containsKey(uuid) || deathLocations.containsKey(uuid);
    }
    public static void clearPreviousLocation(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        previousLocations.remove(uuid);
        deathLocations.remove(uuid);
    }
}
