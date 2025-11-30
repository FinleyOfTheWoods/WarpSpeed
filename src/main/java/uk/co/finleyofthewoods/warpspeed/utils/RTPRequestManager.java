package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.Fluids;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.config.ConfigManager;

import java.util.*;

public class RTPRequestManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTPRequestManager.class);
    private static final Random RANDOM = new Random();

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

    public static BlockPos requestRTP(ServerPlayerEntity player, World world) {
        BlockPos pos = FindBiomeAndPos(world);
        if (pos == null) {
            player.sendMessage(Text.literal("Failed to find a safe location to teleport to."), false);
            return null;
        }
        return pos.up();
    }



    private static BlockPos FindBiomeAndPos(World world) {
        WorldBorder border = world.getWorldBorder();

        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();
        double radius = (size / 2) - 16;

        LOGGER.info("Starting RTP search in world {} with border size {} and radius {}",
                world.getRegistryKey().getValue(), size, radius);

        int maxAttempts = ConfigManager.get().getMaxAttempts();
        boolean isNether = world.getRegistryKey().getValue().toString().contains("the_nether");

        int validBiomeCount = 0;
        int deniedBiomeCount = 0;
        int outsideBorderCount = 0;

        for (int tries = 0; tries < maxAttempts; tries++) {
            double xOffset = (RANDOM.nextDouble() * 2 - 1) * radius;
            double zOffset = (RANDOM.nextDouble() * 2 - 1) * radius;
            int x = (int) (centerX + xOffset);
            int z = (int) (centerZ + zOffset);

            int startY;
            int minY = world.getBottomY();

            // Special handling for Nether dimension
            if (isNether) {
                startY = 120;
                minY = 10;
            } else {
                startY = world.getDimension().logicalHeight() - 1;
            }

            BlockPos pos = new BlockPos(x, startY, z);

            if (!border.contains(pos)) {
                outsideBorderCount++;
                continue;
            }

            RegistryEntry<Biome> biome = world.getBiome(pos);
            String biomeId = biome.getIdAsString();

            if (DENY_BIOMES.contains(biomeId)) {
                deniedBiomeCount++;
                continue;
            }

            validBiomeCount++;

            BlockState state;
            int airGapsFound = 0;
            int solidBlocksFound = 0;
            int lavaBlocksFound = 0;

            for (int y = startY; y > minY; y--) {
                pos = new BlockPos(x, y, z);
                state = world.getBlockState(pos);

                if (state.isOf(Blocks.LAVA) || state.getFluidState().isOf(Fluids.LAVA)) {
                    lavaBlocksFound++;
                }

                if (state.isAir()) {
                    airGapsFound++;
                } else {
                    if (state.isSolidBlock(world, pos)) {
                        solidBlocksFound++;
                    }

                    // Found solid block - check if we have enough air above it
                    if (airGapsFound >= 2 &&
                            state.isSolidBlock(world, pos) &&
                            !state.isOf(Blocks.LAVA) &&
                            !state.getFluidState().isOf(Fluids.LAVA)) {

                        // Additional check: make sure there's no lava immediately above
                        BlockPos abovePos = pos.up();
                        BlockState aboveState = world.getBlockState(abovePos);
                        if (!aboveState.isOf(Blocks.LAVA) &&
                                !aboveState.getFluidState().isOf(Fluids.LAVA)) {

                            LOGGER.debug("Found valid location at ({}, {}, {}) after {} tries. Biome: {}, Air gaps: {}",
                                    x, y, z, tries + 1, biomeId, airGapsFound);
                            return pos;
                        }
                    }
                    // Reset counter for next potential location
                    airGapsFound = 0;
                }
            }

            // Log why this attempt failed
            if (tries < 5 || tries % 50 == 0) {
                LOGGER.debug("Attempt {} at ({}, {}) failed. Biome: {}, Solid: {}, Lava: {}, Max air: {}",
                        tries + 1, x, z, biomeId, solidBlocksFound, lavaBlocksFound, airGapsFound);
            }
        }

        LOGGER.warn("Failed to find RTP location after {} attempts in world {}. Stats: {} valid biomes, {} denied biomes, {} outside border",
                maxAttempts, world.getRegistryKey().getValue(), validBiomeCount, deniedBiomeCount, outsideBorderCount);
        return null;
    }
}
