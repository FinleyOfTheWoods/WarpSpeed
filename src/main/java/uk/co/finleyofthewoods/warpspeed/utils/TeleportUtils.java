package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.Exceptions.NoSafeLocationFoundException;

public class TeleportUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportUtils.class);

    public static boolean teleportToSpawn(ServerPlayerEntity player, World world, BlockPos spawnPos) {
        try {
            LOGGER.debug("Attempting to teleport player {} to spawn at ({}, {}, {})",
                    player.getName().toString(),
                    spawnPos.getX(),
                    spawnPos.getY(),
                    spawnPos.getZ());
            BlockPos safeLoc = findSafeLocation(world, spawnPos);
            return teleportPlayer(player, world, safeLoc);
        } catch (Exception e) {
            handleException(e);
            return false;
        }
    }

    public static boolean teleportToHome(ServerPlayerEntity player, World world, String homeName, DatabaseManager dbManager) {
        try {
            HomePosition home = dbManager.getHome(player.getUuid(), homeName);
            if (home == null) {
                LOGGER.warn("Failed to find home for player {}: {}", player.getName().toString(), homeName);
                return false;
            }
            String currentWorldId = world.getRegistryKey().getValue().toString();
            if (!currentWorldId.equals(home.getWorldId())) {
                LOGGER.warn("Failed to teleport {} to home {}: home is in world {}", player.getName().toString(), homeName, home.getWorldId());
                return false;
            }
            BlockPos homePos = home.getBlockPos();
            LOGGER.debug("Attempting to teleport {} to ({}, {}, {})",
                    player.getName().toString(),
                    homePos.getX(),
                    homePos.getY(),
                    homePos.getZ()
            );
            BlockPos safeLoc = findSafeLocation(world, homePos);
            return teleportPlayer(player, world, safeLoc);
        } catch (Exception e) {
            handleException(e);
            return false;
        }
    }

    public static BlockPos findSafeLocation(World world, BlockPos spawnPos) throws NoSafeLocationFoundException {
        if (isSafeLocation(world, spawnPos)) {
            return spawnPos;
        }
        int searchRadius = 3;
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos pos = spawnPos.add(dx, dy, dz);
                    if (isSafeLocation(world, pos)) {
                        LOGGER.debug("Found safe location at offset ({}, {}, {}) from spawn", dx, dy, dz);
                        return pos;
                    }
                }
            }
        }
        LOGGER.warn("Failed to find safe location within search radius of ({}, {}, {})", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        throw new NoSafeLocationFoundException("Failed to find safe location");
    }

    private static boolean isSafeLocation(World world, BlockPos pos) {
        BlockPos belowPos = pos.down();
        if (!world.getBlockState(belowPos).isSolidBlock(world, belowPos)) {
            return false;
        }
        BlockPos headPos = pos.up();
        return world.getBlockState(pos).isAir() && world.getBlockState(headPos).isAir();
    }

    private static void handleException(Exception e) {
        if (e instanceof NoSafeLocationFoundException nslfe) {
            LOGGER.error("Failed to find safe location: {}", nslfe.getMessage());
        } else {
            LOGGER.error("Unexpected error during teleport: {}", e.getMessage());
        }
    }

    private static boolean teleportPlayer(ServerPlayerEntity player, World world, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        boolean teleported = player.teleport(x, y, z, true);
        LOGGER.debug("Teleport attempt was {} for player {} at ({}, {}, {})",
                teleported, player.getName().toString(), x, y, z);
        if (!teleported) {
            LOGGER.warn("failed to teleport {} to spawn at ({}, {}, {})",
                    player.getName().toString(), x, y, z);
        }
        return teleported;
    }
}
