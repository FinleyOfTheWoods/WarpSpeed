package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
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


public class TeleportUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportUtils.class);

    public static boolean teleportToSpawn(ServerPlayerEntity player, World world, BlockPos spawnPos) {
        try {
            LOGGER.debug("Attempting to teleport player {} to spawn at ({}, {}, {})",
                    player.getName().toString(),
                    spawnPos.getX(),
                    spawnPos.getY(),
                    spawnPos.getZ());
            BlockPos safeLoc = findSafeLocation(world, spawnPos, player);
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

            BlockPos safeLoc = findSafeLocation(targetWorld, homePos, player);
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

            BlockPos safeLoc = findSafeLocation(targetWorld, pos, player);
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

            BlockPos safeLoc = findSafeLocation(targetWorld, warpPos, player);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to warp " + warpName, player);
            return false;
        }
    }

    public static BlockPos findSafeLocation(World world, BlockPos spawnPos, ServerPlayerEntity player) throws NoSafeLocationFoundException {
        if (isSafeLocation(world, spawnPos, player)) {
            return spawnPos;
        }
        int searchRadius = 10;
        // Search in expanding spheres - check nearest positions first
        for (int radius = 1; radius <= searchRadius; radius++) {
            // Check all positions at this distance
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        // Only check positions that are actually at this radius
                        // (on the "shell" of the sphere, not inside it)
                        int maxComponent = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                        if (maxComponent != radius) {
                            continue; // Skip, this position was checked in a previous iteration
                        }

                        BlockPos pos = spawnPos.add(dx, dy, dz);
                        if (isSafeLocation(world, pos, player)) {
                            LOGGER.debug("Found safe location at distance {} with offset ({}, {}, {}) from location",
                                    radius, dx, dy, dz);
                            return pos;
                        }
                    }
                }
            }
        }
        LOGGER.warn("Failed to find safe location within search radius of ({}, {}, {})", spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
        throw new NoSafeLocationFoundException("Failed to find safe location");
    }

    private static boolean isSafeLocation(World world, BlockPos pos, ServerPlayerEntity player) {
        BlockPos belowPos = pos.down();
        BlockPos headPos = pos.up();

        BlockState headState = world.getBlockState(headPos);
        BlockState feetState = world.getBlockState(pos);
        BlockState belowState = world.getBlockState(belowPos);

        boolean hasSafeBase = belowState.isSolidBlock(world, pos)
                || belowState.isOf(Blocks.WATER)
                || !belowState.getFluidState().isEmpty()
                || isSafeToStandIn(belowState, world, belowPos, player);

        if (!hasSafeBase) {
            player.sendMessage(Text.literal("No safe base found"));
            return false;
        }

        boolean isHeadSafe = isSafeToStandIn(headState, world, headPos, player);
        boolean isFeetSafe = isSafeToStandIn(feetState, world, pos, player);
        if (!isHeadSafe || !isFeetSafe) {
            player.sendMessage(Text.literal("Head or feet are unsafe"));
            return false;
        }
        player.sendMessage(Text.literal("Teleporting to safe location"));
        return true;
    }


    private static boolean isSafeToStandIn(BlockState state, World world, BlockPos pos, ServerPlayerEntity player) {
        if (state.isAir()) {
            return true;
        }

        // Safe fluids
        if (state.isOf(Blocks.WATER) || state.getFluidState().isOf(Fluids.WATER)) {
            return true;
        }

        // Sweet berry bushes are dangerous when mature
        if (state.isOf(Blocks.SWEET_BERRY_BUSH)) {
            return false;
        }

        // Dangerous fluids
        if (state.getFluidState().isOf(Fluids.LAVA) || state.getFluidState().isOf(Fluids.FLOWING_LAVA)) {
            player.sendMessage(Text.literal("message.warpspeed.teleport.dangerous_fluid"), false);
            return false;
        }

        // Dangerous blocks where available
        if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE) ||
                state.isOf(Blocks.CACTUS) || state.isOf(Blocks.MAGMA_BLOCK) ||
                state.isOf(Blocks.WITHER_ROSE) || state.isOf(Blocks.POWDER_SNOW) ||
                state.isIn(BlockTags.CAMPFIRES)) {
            return false;
        }

        // Passable/decorative blocks that are safe
        if (state.isIn(BlockTags.WOOL_CARPETS) ||
                state.isIn(BlockTags.FLOWERS) ||
                state.isIn(BlockTags.SAPLINGS) ||
                state.isIn(BlockTags.CROPS) ||
                state.isIn(BlockTags.FLOWER_POTS) ||
                state.isIn(BlockTags.CAVE_VINES) ||
                state.isIn(BlockTags.REPLACEABLE)) {
            return true;
        }

        // Safe blocks to stand on
        if (state.isIn(BlockTags.SLABS)
                || state.isIn(BlockTags.STONE_PRESSURE_PLATES)
                || state.isIn(BlockTags.STONE_BUTTONS)
                || state.isIn(BlockTags.VALID_SPAWN)
                || state.isIn(BlockTags.TRAPDOORS)
                || state.isIn(BlockTags.DOORS)
                || state.isIn(BlockTags.SIGNS)
                || state.isIn(BlockTags.WALL_SIGNS)
                || state.isIn(BlockTags.STAIRS)
                || state.isIn(BlockTags.ALL_SIGNS)
                || state.isIn(BlockTags.ALL_HANGING_SIGNS)
                || state.isIn(BlockTags.WOODEN_SLABS)
                || state.isIn(BlockTags.WOODEN_STAIRS)
                || state.isIn(BlockTags.WOODEN_PRESSURE_PLATES)
                || state.isIn(BlockTags.WOODEN_BUTTONS)
                || state.isIn(BlockTags.WOODEN_TRAPDOORS)
                || state.isIn(BlockTags.WOODEN_DOORS)
                || state.isIn(BlockTags.BANNERS)
                || state.isIn(BlockTags.BEDS)
                || state.isIn(BlockTags.BUTTONS)) {
            return true;
        }

        // Non-solid blocks are not safe to stand in
        return false;
    }


    private static boolean teleportPlayer(ServerPlayerEntity player, World targetWorld, BlockPos pos) {
        // Store current location as previous location
        PlayerLocationTracker.storeCurrentLocation(player);

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        // Check if cross-dimension teleport is needed
        ServerWorld currentWorld = player.getEntityWorld();
        ServerWorld targetServerWorld = (ServerWorld) targetWorld;
        Vec3d currentPos = player.getEntityPos();
        boolean teleported;

        LOGGER.debug("Attempting teleport to ({}, {}, {}) in world {}", x, y, z, targetWorld.getRegistryKey().getValue());

        // Spawn departure particles and sound
        spawnTeleportEffects(currentWorld, currentPos, true);

        try {

            // Cross-dimension teleport
            LOGGER.debug("Cross-dimension teleport from {} to {} for player {}",
                    currentWorld.getRegistryKey().getValue(),
                    targetWorld.getRegistryKey().getValue(),
                    player.getName().getString());

            // Create TeleportTarget for cross-dimension teleportation
            TeleportTarget target = new TeleportTarget(
                    targetServerWorld,
                    new Vec3d(x, y, z),
                    Vec3d.ZERO,  // velocity set to zero
                    player.getYaw(),
                    player.getPitch(),
                    TeleportTarget.NO_OP
            );
            ServerPlayerEntity result = player.teleportTo(target);
            teleported = (result != null);

            LOGGER.debug("Teleport result: {}", teleported);

            if (teleported) {
                // Spawn arrival particles and sound at destination
                spawnTeleportEffects(targetServerWorld, new Vec3d(x, y, z), false);
                player.sendMessage(Text.literal("§aTeleportation successful!"), false);
            } else {
                player.sendMessage(Text.literal("§cTeleportation failed: Unable to complete teleport"), false);
                LOGGER.warn("Teleport returned false for player {} at ({}, {}, {})",
                        player.getName().getString(), x, y, z);
            }

            return teleported;

        } catch (Exception e) {
            player.sendMessage(Text.literal("§cTeleportation failed: " + e.getMessage()), false);
            LOGGER.error("Exception during teleport for player {} to ({}, {}, {}): {}",
                    player.getName().getString(), x, y, z, e.getMessage(), e);
            return false;
        }
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
