package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
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
import net.minecraft.world.border.WorldBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.config.Config;
import uk.co.finleyofthewoods.warpspeed.config.ConfigManager;
import uk.co.finleyofthewoods.warpspeed.infrastructure.HomePosition;
import uk.co.finleyofthewoods.warpspeed.infrastructure.WarpPosition;
import uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions.NoSafeLocationFoundException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static uk.co.finleyofthewoods.warpspeed.Warpspeed.MOD_ID;


public class TeleportUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TeleportUtils.class);
    private static final int TP_REQUEST_COOLDOWN = ConfigManager.get().getTPCooldown();
    private static final Map<UUID, Integer> tpCooldowns = new ConcurrentHashMap<>();
    private static final Config CONFIG = ConfigManager.get();

    public static boolean teleportToSpawn(ServerPlayerEntity player, World world, BlockPos spawnPos) {
        if (isTeleportOnCooldown(player)) {
            return false;
        }
        try {
            LOGGER.debug("[{}] Attempting to teleport player {} to spawn at ({}, {}, {})",
                    MOD_ID,
                    player.getName().toString(),
                    spawnPos.getX(),
                    spawnPos.getY(),
                    spawnPos.getZ());
            BlockPos safeLoc = findSafeLocation(world, spawnPos, player, false);
            return teleportPlayer(player, world, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to spawn", player);
            return false;
        }
    }

    public static boolean teleportToHome(ServerPlayerEntity player, String homeName, DatabaseManager dbManager) {
        if (isTeleportOnCooldown(player)) {
            return false;
        }
        try {
            if (Objects.equals(homeName, "bed")) {
                MinecraftServer server = player.getCommandSource().getServer();
                World overworld = server.getWorld(World.OVERWORLD);
                if (overworld == null) {
                    LOGGER.warn("[{}] Failed to find overworld for player {}: {}", MOD_ID, player.getName().toString(), homeName);
                    return false;
                }
                ServerPlayerEntity.Respawn respawn = player.getRespawn();
                if (respawn == null) {
                    player.sendMessage(Text.literal("No bed position set"), false);
                    return false;
                }
                BlockPos playerBedPos = respawn.respawnData().getPos();
                LOGGER.error("[{}] bed pos: {}", MOD_ID, playerBedPos);
                BlockEntity bedBlockEntity = overworld.getBlockEntity(playerBedPos);
                LOGGER.error("[{}] bed block entity: {}", MOD_ID, bedBlockEntity.toString());
                if (bedBlockEntity == null || bedBlockEntity.isRemoved()) {
                    player.sendMessage(Text.literal("No bed position set"), false);
                    return false;
                }

                LOGGER.debug("[{}] Attempting to teleport {} to bed spawn at ({}, {}, {})",
                        MOD_ID, player.getName().toString(), playerBedPos.getX(), playerBedPos.getY(), playerBedPos.getZ());
                BlockPos safeLoc = findSafeLocation(overworld, playerBedPos, player, false);
                teleportPlayer(player, overworld, safeLoc);
                return true;
            }
            HomePosition home = dbManager.getHome(player.getUuid(), homeName);
            if (home == null) {
                LOGGER.warn("[{}] Failed to find home for player {}: {}", MOD_ID, player.getName().toString(), homeName);
                return false;
            }

            BlockPos homePos = home.getBlockPos();
            LOGGER.debug("[{}] Attempting to teleport {} to ({}, {}, {})",
                    MOD_ID, player.getName().toString(), homePos.getX(), homePos.getY(), homePos.getZ());
            ServerWorld targetWorld = getTargetWorld(player, home.worldId());
            if (targetWorld == null) {
                LOGGER.warn("[{}] Failed to find world {} for warp {}", home.worldId(), homeName);
                player.sendMessage(Text.literal("§cFailed to teleport: warp world not found"), false);
                return false;
            }

            BlockPos safeLoc = findSafeLocation(targetWorld, homePos, player, false);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to home " + homeName, player);
            return false;
        }
    }

    public static boolean teleportToLastLocation(ServerPlayerEntity player) {
        if (isTeleportOnCooldown(player)) {
            return false;
        }
        try {
            if (PlayerLocationTracker.noPreviousLocation(player)) {
                LOGGER.debug("[{}] Failed to teleport {} to last location: no previous location found", MOD_ID, player.getName().toString());
                player.sendMessage(Text.literal("§cNo previous location found"), true);
                return false;
            }
            PlayerLocationTracker.PlayerLocation location = PlayerLocationTracker.getPreviousLocation(player);

            BlockPos pos = location.getBlockPos();
            LOGGER.debug("[{}] Attempt to teleport {} back to ({}, {}, {})", MOD_ID, player.getName().toString(), pos.getX(), pos.getY(), pos.getZ());
            ServerWorld targetWorld = getTargetWorld(player, location.worldId());
            if (targetWorld == null) {
                LOGGER.warn("[{}] Failed to find world {} for previous location warp", MOD_ID, location.worldId());
                player.sendMessage(Text.literal("§cFailed to teleport: warp world not found"), false);
                return false;
            }

            BlockPos safeLoc = findSafeLocation(targetWorld, pos, player, true);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to last location", player);
            return false;
        }
    }

    public static boolean teleportToWarp(ServerPlayerEntity player, String warpName, DatabaseManager dbManager) {
        if (isTeleportOnCooldown(player)) {
            return false;
        }
        try {
            WarpPosition warp = dbManager.getWarp(warpName);
            if (warp == null) {
                LOGGER.warn("[{}] Failed to find warp for player {}: {}", MOD_ID, player.getName().toString(), warpName);
                player.sendMessage(Text.literal("§cFailed to find warp: " + warpName + " does not exist."), true);
                return false;
            }
            if (warp.isPrivate() && !warp.playerUUID().equals(player.getUuid())) {
                LOGGER.warn("[{}] Failed to teleport {} to warp {}: warp is private and not owned by player", MOD_ID, player.getName().toString(), warpName);
                player.sendMessage(Text.literal("§cTeleportation failed. Warp is private and not owned by you."), true);
                return false;
            }
            BlockPos warpPos = warp.getBlockPos();
            LOGGER.debug("[{}] Attempting to teleport {} to ({}, {}, {})",
                    MOD_ID, player.getName().toString(), warpPos.getX(), warpPos.getY(), warpPos.getZ());

            ServerWorld targetWorld = getTargetWorld(player, warp.worldId());
            if (targetWorld == null) {
                LOGGER.warn("[{}] Failed to find world {} for warp {}", MOD_ID, warp.worldId(), warpName);
                player.sendMessage(Text.literal("§cFailed to teleport: warp world not found"), true);
                return false;
            }

            BlockPos safeLoc = findSafeLocation(targetWorld, warpPos, player, false);
            return teleportPlayer(player, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "Failed to teleport to warp " + warpName, player);
            return false;
        }
    }

    public static boolean teleportPlayerToPlayer(ServerPlayerEntity playerToTeleport, ServerPlayerEntity targetPlayer) {
        if (isTeleportOnCooldown(playerToTeleport)) {
            return false;
        }
        try {
            World targetWorld = targetPlayer.getEntityWorld();
            BlockPos targetPos = targetPlayer.getBlockPos();
            LOGGER.debug("[{}] Attempting to teleport to player {} at ({}, {}, {})",
                    MOD_ID,
                    playerToTeleport.getName().toString(),
                    targetPos.getX(),
                    targetPos.getY(),
                    targetPos.getZ());
            playerToTeleport.sendMessage(Text.literal("§6§oFinding a safe location near " + targetPlayer.getName().getString()),true);
            BlockPos safeLoc = findSafeLocation(targetWorld, targetPos, playerToTeleport, false);
            playerToTeleport.sendMessage(Text.literal("§6§oDone. Teleporting now."),true);
            return teleportPlayer(playerToTeleport, targetWorld, safeLoc);
        } catch (Exception e) {
            handleException(e, "§c§oFailed to teleport player", playerToTeleport);
            return false;
        }
    }

    public static boolean teleportToRandomLocation(ServerPlayerEntity player, World world) {
        if (isTeleportOnCooldown(player)) {
            return false;
        }
        LOGGER.debug("[{}] Requesting RTP location for player {} in world {}",
                MOD_ID, player.getName().getString(), world.getRegistryKey().getValue());
        BlockPos pos = RTPRequestManager.requestRTP(player, world);
        if (pos == null) {
            LOGGER.warn("[{}] RTP request returned null position for player {} in world {}",
                    MOD_ID, player.getName().getString(), world.getRegistryKey().getValue());
            // RTPRequestManager already sent the error message to the player
            return false;
        }
        try {
            BlockPos safePos = findSafeLocation(world, pos, player, false);
            if (teleportPlayer(player, world, safePos)) {
                return true;
            } else {
                player.sendMessage(Text.literal("Failed to find a suitable RTP location."), true);
                return false;
            }
        } catch (Exception e) {
            handleException(e, "§c§oFailed to find a safe RTP location", player);
            return false;
        }
    }

    private static boolean teleportPlayer(ServerPlayerEntity player, World targetWorld, BlockPos pos) {
        // Store current location as previous location
        PlayerLocationTracker.storeCurrentLocation(player);

        // force player to dismount their mount.
        player.stopRiding();
        // force player to stop gliding.
        player.stopGliding();
        // force player to stop using item.
        player.stopUsingItem();

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1;
        double z = pos.getZ() + 0.5;

        // Check if cross-dimension teleport is needed
        ServerWorld currentWorld = player.getEntityWorld();
        ServerWorld targetServerWorld = (ServerWorld) targetWorld;
        Vec3d currentPos = player.getEntityPos();
        boolean teleported;

        LOGGER.debug("[{}] Attempting teleport to ({}, {}, {}) in world {}", MOD_ID, x, y, z, targetWorld.getRegistryKey().getValue());

        // Spawn departure particles and sound
        spawnTeleportEffects(currentWorld, currentPos, true);

        try {
            // Cross-dimension teleport
            LOGGER.debug("[{}] Cross-dimension teleport from {} to {} for player {}",
                    MOD_ID,
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

            LOGGER.debug("[{}] Teleport result: {}", MOD_ID, teleported);

            if (teleported) {
                // Spawn arrival particles and sound at destination
                spawnTeleportEffects(targetServerWorld, new Vec3d(x, y, z), false);
                player.sendMessage(Text.literal("§aTeleportation successful!"), true);
                setTPCooldown(player);
            } else {
                player.sendMessage(Text.literal("§cTeleportation failed: Unable to complete teleport"), true);
                LOGGER.warn("[{}] Teleport returned false for player {} at ({}, {}, {})",
                        MOD_ID, player.getName().getString(), x, y, z);
            }

            return teleported;
        } catch (Exception e) {
            handleException(e, "Failed to teleport.", player);
            return false;
        }
    }

    private static void handleException(Exception e, String message, ServerPlayerEntity player) {
        if (e instanceof NoSafeLocationFoundException nslfe) {
            LOGGER.error("[{}] Failed to find safe location: {}", MOD_ID, nslfe.getMessage());
            player.sendMessage(Text.literal(nslfe.getMessage() + ": " + message), false);
        } else {
            LOGGER.error("[{}] Unexpected error during teleport: {}", MOD_ID, e.getMessage());
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
            LOGGER.error("[{}] Server is null for player {}", MOD_ID, player.getName().toString());
            return null;
        }
        // Parse the world ID and get the world
        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.tryParse(targetWorldId));
        return server.getWorld(worldKey);
    }

    private static boolean isTeleportOnCooldown(ServerPlayerEntity player) {
        int timeRemaining = hasTPRequestCooldownExpired(player);
        if (timeRemaining > 0) {
            player.sendMessage(Text.literal("§cYou must wait " + timeRemaining + " seconds before teleporting again"), true);
            return true;
        }
        return false;
    }

    private static int hasTPRequestCooldownExpired(ServerPlayerEntity player) {
        if (tpCooldowns.containsKey(player.getUuid())) {
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            int timeSinceLastRequest = timestamp - tpCooldowns.get(player.getUuid());
            return TP_REQUEST_COOLDOWN - timeSinceLastRequest;
        }
        return 0;
    }

    private static void setTPCooldown(ServerPlayerEntity player) {
        tpCooldowns.put(player.getUuid(), (int) (System.currentTimeMillis() / 1000));
    }

    private static BlockPos findSafeLocation(World world, BlockPos pos, ServerPlayerEntity player, boolean isBackCommand) throws NoSafeLocationFoundException {
        LOGGER.error("{}", CONFIG.toString());
        if (isSafeLocation(world, pos, player, isBackCommand)) return pos;
        BlockPos safePOS;
        int searchRadius = 10;

        for (int radius = 1; radius <= searchRadius; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        int maxComponent = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                        if (maxComponent != radius) {
                            continue; // Skip, this position was checked in a previous iteration
                        }
                        safePOS = pos.add(dx, dy, dz).up();
                        if (isSafeLocation(world, safePOS, player, isBackCommand)) {
                            LOGGER.debug("Found safe location at distance {} with offset ({}, {}, {}) from location",
                                    radius, dx, dy, dz);
                            return safePOS;
                        }
                    }
                }
            }
        }
        throw new NoSafeLocationFoundException("No safe location found within " + searchRadius + " blocks of (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")");
    }

    private static boolean isSafeLocation(World world, BlockPos pos, ServerPlayerEntity player, boolean isBackCommand) {
        LOGGER.debug("[{}] Checking if ({}, {}, {}) is safe.", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        LOGGER.debug("[{}] Checking if ({}, {}, {}) is inside world border.", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        if (!isInsideWorld(world, pos)) return false;
        LOGGER.debug("[{}] Checking if ({}, {}, {}) has enough space for player.", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        if (!canFitPlayer(world, pos, isBackCommand)) return false;
        LOGGER.debug("[{}] Checking if ({}, {}, {}) is safe to stand on.", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        if (!canSurviveFall(world, pos, player, isBackCommand)) return false;
        LOGGER.debug("[{}] Checking if ({}, {}, {}) is safe to stand on (after falling).", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        return isSafeToStandIn(world.getBlockState(pos.down()), isBackCommand);
    }

    private static boolean isInsideWorld(World world, BlockPos pos) {
        WorldBorder border = world.getWorldBorder();
        boolean isInsideBorder = border.contains(pos) && pos.getY() >= world.getBottomY();
        LOGGER.debug("[{}] World border contains ({}, {})? {}", MOD_ID, pos.getX(), pos.getZ(), isInsideBorder);
        return isInsideBorder;
    }


    private static boolean canFitPlayer(World world, BlockPos feetPos, boolean isBackCommand) {
        BlockPos headPos = feetPos.up();
        BlockState headPosBlockState = world.getBlockState(headPos);
        BlockState feetPosBlockState = world.getBlockState(feetPos);

        LOGGER.debug("[{}] Head block state: {}, feet block state: {}", MOD_ID, headPosBlockState.getBlock().getName(), feetPosBlockState.getBlock().getName());

        // Special case: fully submerged in water is OK
        if (feetPosBlockState.isOf(Blocks.WATER) && headPosBlockState.isOf(Blocks.WATER)) return true;
        if (feetPosBlockState.getFluidState().isOf(Fluids.FLOWING_WATER) &&
                headPosBlockState.getFluidState().isOf(Fluids.FLOWING_WATER)) return true;

        // Special case: both airs are OK
        if (feetPosBlockState.isAir() && headPosBlockState.isAir()) return true;

        // Special case: leaves block player
        if (feetPosBlockState.isIn(BlockTags.LEAVES) && headPosBlockState.isIn(BlockTags.LEAVES)) return false;

        // General case: both must be non-solid AND safe
        boolean feetNonSolid = !feetPosBlockState.isSolidBlock(world, feetPos);
        boolean headNonSolid = !headPosBlockState.isSolidBlock(world, headPos);
        boolean feetSafe = isSafeToStandIn(feetPosBlockState, isBackCommand);
        boolean headSafe = isSafeToStandIn(headPosBlockState, isBackCommand);

        return feetNonSolid && headNonSolid && feetSafe && headSafe;
    }

    private static boolean isSafeToStandIn(BlockState state, boolean isBackCommand) {
        LOGGER.debug("[{}] Checking if block state ({}) is safe to stand in.", MOD_ID, state.getBlock().getName());

        /// Check if Lava; Disable in config
        if(state.getFluidState().isOf(Fluids.LAVA) || state.getFluidState().isOf(Fluids.FLOWING_LAVA) || state.isOf(Blocks.LAVA)
                || (!state.getFluidState().isEmpty() && state.getFluidState().isIn(FluidTags.LAVA))) {
            if (isBackCommand) {
                return !CONFIG.isLavaCheckEnabled();
            }
            return false;
        }

        /// Check if Fire. Disable in config
        if (state.isOf(Blocks.FIRE) || state.isOf(Blocks.SOUL_FIRE) || state.isOf(Blocks.CAMPFIRE)) {
            if (isBackCommand) return !CONFIG.isFireCheckEnabled(); // true if disabled, false if enabled
            return false;
        }

        ///  Check if Wither Rose; Disable in config
        if (state.isOf(Blocks.WITHER_ROSE)) {
            if (isBackCommand) return !CONFIG.isWitherRoseCheckEnabled(); // true if disabled, false if enabled
            return false;
        }

        /// Check if Cactus; Disable in config
        if (state.isOf(Blocks.CACTUS)) {
            if (isBackCommand) return !CONFIG.isCactusCheckEnabled(); // true if disabled, false if enabled
            return false;
        }

        /// Check if Magma Block; Disable in config
        if (state.isOf(Blocks.MAGMA_BLOCK)) {
            if (isBackCommand) return !CONFIG.isMagmaBlockCheckEnabled(); // true if disabled, false if enabled
            return false;
        }

        /// Check if Sweet Berry Bush; Disable in config
        if (state.isOf(Blocks.SWEET_BERRY_BUSH)) {
            if (isBackCommand) return !CONFIG.isSweetBerryBushCheckEnabled(); // true if disabled, false if enabled
            return false;
        }

        /// Check if Void. Cannot disable.
        return !state.isOf(Blocks.VOID_AIR);
    }

    private static boolean canSurviveFall(World world, BlockPos pos, ServerPlayerEntity player, boolean isBackCommand) {
        if (isBackCommand && !CONFIG.isFallCheckEnabled()) return true; // true if disabled, false if enabled

        LOGGER.debug("[{}] Checking if player can survive falling from ({}, {}, {})", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        if (isWearingElytra(player)) return true;
        boolean isSafe = false;
        for (int i = 1; i < 10; i++) {
            BlockState state = world.getBlockState(pos.down(i));
            // Check if it's solid ground OR safe liquid (water)
            boolean isSolidGround = state.isSolidBlock(world, pos.down(i));
            boolean isSafeLiquid = !state.isAir() && state.getFluidState().isOf(Fluids.WATER);
            boolean isSafeToStand = isSafeToStandIn(state, isBackCommand);
            if (isSolidGround || isSafeLiquid) {
                LOGGER.debug("[{}] Found safe landing {} blocks below: {}", MOD_ID, i, state.getBlock().getName());
                isSafe = true;
            }
            if (!isSafeToStand) {
                isSafe = false;
            }
        }
        LOGGER.debug("[{}] No solid blocks found below pos({}, {}, {}).", MOD_ID, pos.getX(), pos.getY(), pos.getZ());
        return isSafe;
    }

    private static boolean isWearingElytra(ServerPlayerEntity player){
        LOGGER.debug("[{}] Checking if player is wearing an elytra.", MOD_ID);
        boolean isWearingElytra = player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA;
        LOGGER.debug("[{}] Player is wearing an elytra? {}", MOD_ID, isWearingElytra);
        return isWearingElytra;
    }

}
