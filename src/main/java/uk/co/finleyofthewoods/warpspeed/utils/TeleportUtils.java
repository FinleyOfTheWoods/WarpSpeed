package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.exceptions.NoSafeLocationFoundException;

import java.util.Arrays;
import java.util.List;

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
            handleException(e, "Failed to teleport to spawn", player);
            return false;
        }
    }

    public static boolean teleportToHome(ServerPlayerEntity player, World world, String homeName, DatabaseManager dbManager) {
        try {
            HomePosition home = dbManager.getHome(player.getUuid(), homeName);
            if (home == null) {
                player.sendMessage(Text.literal("§cFailed to find home: " + homeName), false);
                LOGGER.warn("Failed to find home for player {}: {}", player.getName().toString(), homeName);
                return false;
            }

            BlockPos homePos = home.getBlockPos();
            LOGGER.debug("Attempting to teleport {} to ({}, {}, {})",
                    player.getName().toString(), homePos.getX(), homePos.getY(), homePos.getZ());
            ServerWorld targetWorld = getTargetWorld(player, home.getWorldId());
            if (targetWorld == null) {
                LOGGER.warn("Failed to find world {} for warp {}", home.getWorldId(), homeName);
                player.sendMessage(Text.literal("§cFailed to teleport: warp world not found"), false);
                return false;
            }

            BlockPos safeLoc = findSafeLocation(targetWorld, homePos);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to home " + homeName, player);
            return false;
        }
    }

    public static boolean teleportToLastLocation(ServerPlayerEntity player) {
        try {
            if (!PlayerLocationTracker.hasPreviousLocation(player)) {
                LOGGER.debug("Failed to teleport {} to last location: no previous location found", player.getName().toString());
                player.sendMessage(Text.literal("§cFailed to teleport: no previous location found"), false);
                return false;
            }
            PlayerLocationTracker.PlayerLocation location = PlayerLocationTracker.getPreviousLocation(player);

            BlockPos pos = location.getBlockPos();
            LOGGER.debug("Attempt to teleport {} back to ({}, {}, {})", player.getName().toString(), pos.getX(), pos.getY(), pos.getZ());
            ServerWorld targetWorld = getTargetWorld(player, location.getWorldId());
            if (targetWorld == null) {
                LOGGER.warn("Failed to find world {} for previous location warp", location.getWorldId());
                player.sendMessage(Text.literal("§cFailed to teleport: warp world not found"), false);
                return false;
            }

            BlockPos safeLoc = findSafeLocation(targetWorld, pos);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to last location", player);
            return false;
        }
    }

    public static boolean teleportToWarp(ServerPlayerEntity player, World world, String warpName, DatabaseManager dbManager) {
        try {
            WarpPosition warp = dbManager.getWarp(warpName);
            if (warp == null) {
                LOGGER.warn("Failed to find warp for player {}: {}", player.getName().toString(), warpName);
                player.sendMessage(Text.literal("§cFailed to find warp: " + warpName + " does not exist."), false);
                return false;
            }
            if (warp.isPrivate() && !warp.getPlayerUUID().equals(player.getUuid())) {
                LOGGER.warn("Failed to teleport {} to warp {}: warp is private and not owned by player", player.getName().toString(), warpName);
                player.sendMessage(Text.literal("§cTeleportation failed. Warp is private and not owned by you."), false);
                return false;
            }
            BlockPos warpPos = warp.getBlockPos();
            LOGGER.debug("Attempting to teleport {} to ({}, {}, {})",
                    player.getName().toString(), warpPos.getX(), warpPos.getY(), warpPos.getZ());

            ServerWorld targetWorld = getTargetWorld(player, warp.getWorldId());
            if (targetWorld == null) {
                LOGGER.warn("Failed to find world {} for warp {}", warp.getWorldId(), warpName);
                player.sendMessage(Text.literal("§cFailed to teleport: warp world not found"), false);
                return false;
            }

            BlockPos safeLoc = findSafeLocation(targetWorld, warpPos);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to warp " + warpName, player);
            return false;
        }
    }

    public static BlockPos findSafeLocation(World world, BlockPos spawnPos) throws NoSafeLocationFoundException {
        if (isSafeLocation(world, spawnPos)) {
            return spawnPos;
        }
        int searchRadius = 10;
        for (int dx = -searchRadius; dx <= searchRadius; dx++) {
            for (int dy = -searchRadius; dy <= searchRadius; dy++) {
                for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                    BlockPos pos = spawnPos.add(dx, dy, dz);
                    if (isSafeLocation(world, pos)) {
                        LOGGER.debug("Found safe location at offset ({}, {}, {}) from location", dx, dy, dz);
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
        BlockPos headPos = pos.up();

        BlockState headState = world.getBlockState(headPos);
        BlockState feetState = world.getBlockState(pos);
        BlockState belowState = world.getBlockState(belowPos);

        boolean hasSafeBase = belowState.isSolidBlock(world, belowPos)
                || belowState.isOf(Blocks.WATER)
                || !belowState.isAir() && isSafeToStandOn(belowState, world, belowPos);

        if (!hasSafeBase) {
            return false;
        }

        boolean isHeadSafe = isSafeToStandIn(headState, world, headPos);
        boolean isFeetSafe = isSafeToStandIn(feetState, world, pos);

        return isHeadSafe && isFeetSafe;
    }

    private static final List<Block> carpets = Arrays.asList(
            Blocks.WHITE_CARPET,
            Blocks.ORANGE_CARPET,
            Blocks.MAGENTA_CARPET,
            Blocks.LIGHT_BLUE_CARPET,
            Blocks.YELLOW_CARPET,
            Blocks.LIME_CARPET,
            Blocks.PINK_CARPET,
            Blocks.GRAY_CARPET,
            Blocks.LIGHT_GRAY_CARPET,
            Blocks.CYAN_CARPET,
            Blocks.PURPLE_CARPET,
            Blocks.BLUE_CARPET,
            Blocks.BROWN_CARPET,
            Blocks.GREEN_CARPET,
            Blocks.RED_CARPET,
            Blocks.BLACK_CARPET
    );

    private static boolean isSafeToStandOn(BlockState state, World world, BlockPos pos) {
        for (Block carpet : carpets) {
            if (state.isOf(carpet)) {
                return true;
            }
        }
        // Check for slabs (they're not full solid blocks)
        // This catches slabs, pressure plates, etc.
        return !state.isSolidBlock(world, pos) && !state.isAir();
    }

    private static boolean isSafeToStandIn(BlockState state, World world, BlockPos pos) {
        if (state.isAir()) {
            // Air is safe to stand in
            return true;
        } else if (state.isOf(Blocks.WATER) || state.getFluidState().isOf(Fluids.WATER)) {
            // Water is safe to stand
            return true;
        } else if (state.getFluidState().isOf(Fluids.LAVA) || state.getFluidState().isOf(Fluids.FLOWING_LAVA)) {
            // Lava is not safe to stand in
            return false;
        } else if (state.isOf(Blocks.SWEET_BERRY_BUSH) || state.isOf(Blocks.CACTUS)) {
            // Cactus and sweet berry bushes are not safe to stand in
            return false;
        } else if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE)) {
            // Fire and soul fire are not safe to stand in
            return false;
        } else if (!state.isSolidBlock(world, pos)) {
            // Non-solid blocks (carpets, slabs, flowers, tall grass, etc.) are safe to stand in/on
            return true;
        }
        // Non-solid blocks are not safe to stand in
        return false;
    }

    private static boolean teleportPlayer(ServerPlayerEntity player, World targetWorld, BlockPos pos) {
        // Store current location as previous location. To allow ping-ponging between two locations using /back repeatedly.
        PlayerLocationTracker.storeCurrentLocation(player);


        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        // Check if cross-dimension teleport is needed
        ServerWorld currentWorld = player.getEntityWorld();
        ServerWorld targetServerWorld = (ServerWorld) targetWorld;
        Vec3d currentPos = player.getEntityPos();
        boolean teleported;

        // Spawn departure particles and sound
        spawnTeleportEffects(currentWorld, currentPos, true);
        if (currentWorld.getRegistryKey() != targetWorld.getRegistryKey()) {
            // Cross-dimension teleport
            LOGGER.debug("Cross-dimension teleport from {} to {} for player {}",
                    currentWorld.getRegistryKey().getValue(),
                    targetWorld.getRegistryKey().getValue(),
                    player.getName().toString());

            // Create TeleportTarget for cross-dimension teleportation
            TeleportTarget target = new TeleportTarget(
                    targetServerWorld,
                    new Vec3d(x, y, z),
                    Vec3d.ZERO,  // velocity set to zero to prevent the player from moving during cross-dimension teleportation
                    player.getYaw(),
                    player.getPitch(),
                    TeleportTarget.NO_OP
            );
            ServerPlayerEntity result = player.teleportTo(target);
            teleported = (result != null);
        } else {
            // Same dimension teleport
            teleported = player.teleport(x, y, z, false);
        }
        LOGGER.debug("Teleport attempt was {} for player {} at ({}, {}, {})",
                teleported, player.getName().toString(), x, y, z);
        if (!teleported) {
            LOGGER.warn("failed to teleport {} to spawn at ({}, {}, {})",
                    player.getName().toString(), x, y, z);
        }
        if (teleported) {
            // Spawn arrival particles and sound at destination
            spawnTeleportEffects(targetServerWorld, new Vec3d(x, y, z), false);
        }
        return teleported;
    }

    private static void handleException(Exception e, String message, ServerPlayerEntity player) {
        if (e instanceof NoSafeLocationFoundException nslfe) {
            LOGGER.error("Failed to find safe location: {}", nslfe.getMessage());
            player.sendMessage(Text.literal(nslfe.getMessage() + ": " + message), false);
        } else {
            LOGGER.error("Unexpected error during teleport: {}", e.getMessage());
            player.sendMessage(Text.literal("Unexpected error during teleport: " + message), false);
        }
    }

    /**
     * Spawns particle effects and plays sounds for teleportation
     * @param world The world to spawn effects in
     * @param pos The position to spawn effects at
     * @param isDeparture True for departure effects, false for arrival effects
     */
    private static void spawnTeleportEffects(ServerWorld world, Vec3d pos, boolean isDeparture) {
        // Spawn particle effects
        if (isDeparture) {
            // Departure: Purple portal particles spiraling upward
            for (int i = 0; i < 50; i++) {
                double offsetX = (Math.random() - 0.5) * 1.5;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 1.5;

                world.spawnParticles(
                        ParticleTypes.PORTAL,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        1,  // count
                        0.2, // deltaX
                        0.5, // deltaY
                        0.2, // deltaZ
                        0.1  // speed
                );
            }

            // Additional enchant glint effect
            for (int i = 0; i < 20; i++) {
                double offsetX = (Math.random() - 0.5);
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5);

                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        1,
                        0, 0, 0, 0
                );
            }

            // Play departure sound
            world.playSound(
                    null,  // player (null = everyone hears it)
                    pos.x, pos.y, pos.z,
                    SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                    SoundCategory.PLAYERS,
                    1.0f,  // volume
                    1.0f   // pitch
            );
        } else {
            // Arrival: Explosion of particles
            for (int i = 0; i < 50; i++) {
                double offsetX = (Math.random() - 0.5) * 1.5;
                double offsetY = Math.random() * 2;
                double offsetZ = (Math.random() - 0.5) * 1.5;

                world.spawnParticles(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        pos.x + offsetX,
                        pos.y + offsetY,
                        pos.z + offsetZ,
                        1,
                        0.2, 0.5, 0.2, 0.1
                );
            }

            // Poof effect on arrival
            world.spawnParticles(
                    ParticleTypes.POOF,
                    pos.x, pos.y + 0.5, pos.z,
                    30,  // count
                    0.5, 0.5, 0.5,  // delta
                    0.05  // speed
            );

            // Play arrival sound
            world.playSound(
                    null,
                    pos.x, pos.y, pos.z,
                    SoundEvents.ITEM_CHORUS_FRUIT_TELEPORT,
                    SoundCategory.PLAYERS,
                    1.0f,
                    1.2f  // slightly higher pitch for arrival
            );
        }
    }

    private static ServerWorld getTargetWorld(ServerPlayerEntity player, String targetWorldId) {
        // Get the target world from the server
        MinecraftServer server = player.getCommandSource().getServer();
        if (server == null) {
            LOGGER.error("Server is null for player {}", player.getName().toString());
            return null;
        }
        // Parse the world ID and get the world
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(targetWorldId));
        ServerWorld targetWorld = server.getWorld(worldKey);
        return targetWorld;
    }
}
