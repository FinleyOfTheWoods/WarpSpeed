package uk.co.finleyofthewoods.warpspeed.manager.impl;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData.RespawnData;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.exception.TeleportFailureException;
import uk.co.finleyofthewoods.warpspeed.manager.TeleportManager;
import uk.co.finleyofthewoods.warpspeed.model.BaseLocation;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;
import uk.co.finleyofthewoods.warpspeed.model.WarpLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
public class TeleportManagerImpl implements TeleportManager {
    private static final LocationManagerImpl locationManager = new LocationManagerImpl();

    private static final List<String> DENY_BIOMES = new ArrayList<>() {{
        add("minecraft:deep_cold_ocean");
        add("minecraft:deep_frozen_ocean");
        add("minecraft:deep_lukewarm_ocean");
        add("minecraft:deep_ocean");
        add("minecraft:cold_ocean");
        add("minecraft:frozen_ocean");
        add("minecraft:lukewarm_ocean");
        add("minecraft:ocean");
        add("minecraft:warm_ocean");
        add("minecraft:small_end_islands");
        add("minecraft:the_end");
        add("minecraft:the_void");
        add("minecraft:river");
        add("minecraft:frozen_river");
        add("minecraft:beach");
    }};

    private boolean teleport(@NonNull ServerPlayer player, @NonNull HomeLocation location) {
        try {
            log.debug("Teleporting {} to home {} ({})", player.getDisplayName(), location.getName(), location.getPos());
            ServerLevel level = location.getLevel();
            BlockPos pos = location.getPos();
            return teleportPlayerToPosition(player, pos, level);
        } catch (TeleportFailureException e ){
            log.error("Failed to teleport player: {} to location: {} ({})", player.getPlainTextName(), location.getName(), location.getPos(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during teleporting player {} to location {} ({})", player.getPlainTextName(), location.getName(), location.getPos(), e);
            return false;
        }
    }

    private boolean teleport(@NonNull ServerPlayer player, @NonNull BaseLocation location) {
        try {
            log.debug("Teleporting {} to {}", player.getDisplayName(), location.getPos());
            ServerLevel level = location.getLevel();
            BlockPos pos = location.getPos();
            return teleportPlayerToPosition(player, pos, level);
        } catch (TeleportFailureException e ){
            log.error("Failed to teleport player: {} to location: {}", player.getPlainTextName(), location.getPos(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during teleporting player {} to location {}", player.getPlainTextName(), location.getPos(), e);
            return false;
        }
    }

    private boolean teleport(@NonNull ServerPlayer player, @NonNull WarpLocation location) {
        try {
            log.debug("Teleporting {} to warp {} ({})", player.getDisplayName(), location.getName(), location.getPos());
            ServerLevel level = location.getLevel();
            BlockPos pos = location.getPos();
            return teleportPlayerToPosition(player, pos, level);
        } catch (TeleportFailureException e ){
            log.error("Failed to teleport player: {} to location: {} ({})", player.getPlainTextName(), location.getName(), location.getPos(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during teleporting player {} to location {} ({})", player.getPlainTextName(), location.getName(), location.getPos(), e);
            return false;
        }
    }

    private boolean teleportPlayerToPosition(@NonNull ServerPlayer player, @NonNull BlockPos pos, @NonNull ServerLevel level) throws TeleportFailureException {
       TeleportTransition transition = new TeleportTransition(
                level,
                new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                new Vec3(0, 0, 0),
                0,
                0,
                TeleportTransition.DO_NOTHING
        );
        if (player.teleport(transition) != null) {
            // Player was teleported, play Ender man Teleport sound and return true
            log.debug("Teleported player {} to position {}", player.getPlainTextName(), pos);
            player.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return true;
        } else {
            // Player was not teleported, play Candle Extinguish sound and throw exception
            log.error("Failed to teleport player {} to position {}", player.getPlainTextName(), pos);
            player.playSound(SoundEvents.CANDLE_EXTINGUISH, 1.0f, 1.0f);
            throw new TeleportFailureException("Failed to teleport player to location");
        }
    }

    private boolean attemptRandomTeleport(@NonNull ServerPlayer player, @NonNull ServerLevel level, @NonNull Random random, int attempt) {
        if (attempt >= 100) {
            log.debug("Failed to find safe location after 100 attempts");
            return false;
        }
        WorldBorder border = level.getWorldBorder();
        int x = (int) (border.getMinX() + random.nextDouble() * border.getSize());
        int z = (int) (border.getMinZ() + random.nextDouble() * border.getSize());
        log.debug("Attempting to teleport player {} to random location: {} {}", player.getName().getString(), x, z);

        level.getChunkSource().getChunkFuture(x >> 4, z >> 4, ChunkStatus.SURFACE, true)
                .thenAccept(chunkResult -> {
                    ChunkAccess chunk = chunkResult.orElse(null);
                    if (chunk == null) {
                        log.debug("Chunk is null, attempting new random teleport");
                        attemptRandomTeleport(player, level, random, attempt + 1);
                        return;
                    }
                    String biome = level.getBiome(new BlockPos(x, 64, z)).unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
                    if (DENY_BIOMES.contains(biome)) {
                        log.debug("Biome {} is in deny list, attempting new random teleport", biome);
                        attemptRandomTeleport(player, level, random, attempt + 1);
                        return;
                    }
                    int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
                    BlockPos teleportPos = new BlockPos(x, y + 1, z);
                    if (!isSafe(player, teleportPos, level)) {
                        log.debug("Location {} is not safe, attempting new random teleport", teleportPos);
                        attemptRandomTeleport(player, level, random, attempt + 1);
                        return;
                    }
                    log.debug("Location {} is safe, teleporting player", teleportPos);
                    BaseLocation location = new BaseLocation(null, teleportPos, level);
                    teleport(player, location);
                });
        return true;
    }

    private boolean isSafe(@NonNull ServerPlayer player, @Nullable BlockPos pos, @NonNull ServerLevel level) {
        if (pos == null) return false;
        if (player.isCreative() || player.isSpectator()) return true;
        if (!level.isInWorldBounds(pos)) return false;
        if (!level.canSeeSky(pos) && !level.canSeeSkyFromBelowWater(pos)) return false;
        if (!level.isInsideBuildHeight(pos)) return false;
        BlockState blockState = level.getBlockState(pos);
        if (!blockState.entityCanStandOn(level, pos, player)) return false;
        if (!blockState.canSurvive(level, pos)) return false;
        if (!blockState.isValidSpawn(level, pos, player.getLivingEntity().getType())) return false;
        if (blockState.is(Blocks.LAVA) && !player.fireImmune()) return false;
        if ((blockState.is(Blocks.FIRE)|| blockState.is(Blocks.SOUL_FIRE)) && !player.fireImmune()) return false;
        if ((blockState.is(Blocks.CAMPFIRE) || blockState.is(Blocks.SOUL_CAMPFIRE)) && !player.fireImmune()) return false;
        if (blockState.is(Blocks.MAGMA_BLOCK) && !player.fireImmune()) return false;
        if (blockState.is(Blocks.SWEET_BERRY_BUSH)) return false;
        if (blockState.is(Blocks.VOID_AIR)) return false;
        return level.isWaterAt(pos);
    }

    @Override
    public boolean teleportHome(@NonNull ServerPlayer player, @NonNull String name) {
        HomeLocation location;
        if (name.equals("bed")) {
            location = locationManager.getBedLocation(player);
        } else {
            location = locationManager.getHomeLocationByName(player, name);
        }
        if (location == null) {
            log.debug("Home location {} not found for player {}", name, player.getPlainTextName());
            player.sendSystemMessage(Component.literal("Home location not found").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return teleport(player, location);
    }

    @Override
    public boolean teleportSpawn(@NonNull ServerPlayer player) {
        ServerLevel level = player.level();
        MinecraftServer server = level.getServer();
        RespawnData respawnData = server.getRespawnData();
        BlockPos pos = respawnData.pos();
        ServerLevel spawnLevel = server.getLevel(respawnData.dimension());

        BaseLocation location = new BaseLocation(null, pos, spawnLevel);
        return teleport(player, location);
    }

    @Override
    public boolean teleportWarp(@NonNull ServerPlayer player, @NonNull String name) {
        WarpLocation location = locationManager.getWarpLocationByName(player, name);
        if (location == null) {
            log.debug("Warp location {} not found for player {}", name, player.getPlainTextName());
            player.sendSystemMessage(Component.literal("Warp location not found").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return teleport(player, location);
    }

    @Override
    public boolean teleportRandomly(@NonNull ServerPlayer player) {
        log.debug("teleporting player {} to random location", player.getPlainTextName());
        ServerLevel level = player.level();
        return attemptRandomTeleport(player, level, new Random(), 0);
    }

    @Override
    public boolean teleportBack(@NonNull ServerPlayer player) {
        BaseLocation location = locationManager.getPreviousLocation(player);
        if (location == null) {
            log.debug("No previous location found for player {}", player.getPlainTextName());
            player.sendSystemMessage(Component.literal("No previous location found")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        return teleport(player, location);
    }

    public boolean teleportToPlayer(@NonNull ServerPlayer player, @NonNull ServerPlayer target) {
        return teleport(player, new BaseLocation(null, target.blockPosition(), target.level()));
    }
}
