package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.Exceptions.NoSafeLocationFoundException;

public class TeleportUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger("TeleportUtils");

    public static boolean teleportToSpawn(ServerPlayerEntity player, World world, BlockPos spawnPos) {
        try {
            LOGGER.debug("Attempting to teleport player {} to spawn at ({}, {}, {})",
                    player.getName().toString(),
                    spawnPos.getX(),
                    spawnPos.getY(),
                    spawnPos.getZ());
            BlockPos safeLoc = findSafeLocation(world, spawnPos);
            boolean teleported = player.teleport(
                    safeLoc.getX() + 0.5,
                    safeLoc.getY(),
                    safeLoc.getZ() + 0.5,
                    true
            );
            if (!teleported) {
                LOGGER.warn("failed to teleport {} to spawn at ({}, {}, {})",
                        player.getName().toString(),
                        safeLoc.getX(),
                        safeLoc.getY(),
                        safeLoc.getZ()
                );
            }
            return teleported;
        } catch (NoSafeLocationFoundException e) {
            LOGGER.warn("Failed to find safe location for player {}", player.getName().toString());
            return false;
        } catch (Exception e) {
            LOGGER.error("Exception during teleport for player {}", player.getName().toString(), e);
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
}
