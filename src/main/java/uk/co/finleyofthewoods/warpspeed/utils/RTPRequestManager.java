package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.border.WorldBorder;

import java.util.*;

public class RTPRequestManager {
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
    }};

    public static BlockPos requestRTP(ServerPlayerEntity player, World world) {
        BlockPos pos = FindBiomeAndPos(world);
        if (pos == null) {
            player.sendMessage(Text.literal("Failed to find a safe location to teleport to."), false);
            return null;
        }
        player.sendMessage(Text.literal("Teleporting to new location... " + pos.toShortString()), false);

        return pos.up();
    }

    private static BlockPos FindBiomeAndPos(World world) {
        WorldBorder border = world.getWorldBorder();

        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();

        double radius = (size / 2) - 16;

        boolean posFound = false;

        for (int tries = 0; tries < 20 && !posFound; tries++) {
            double xOffset = (RANDOM.nextDouble() * 2 - 1) * radius;
            double zOffset = (RANDOM.nextDouble() * 2 - 1) * radius;
            int x = (int) (centerX + xOffset);
            int z = (int) (centerZ + zOffset);
            int y = world.getHeight() - world.getBottomY();
            BlockPos pos = new BlockPos(x, y, z);

            RegistryEntry<Biome> biome = world.getBiome(pos);
            String biomeId = biome.getIdAsString();

            if (!DENY_BIOMES.contains(biomeId)) {
                BlockState state = world.getBlockState(pos);
                while (state.isAir()) {
                    pos = pos.down(4);
                    state = world.getBlockState(pos);
                }
                return pos;
            }
        }
        return null;
    }
}
