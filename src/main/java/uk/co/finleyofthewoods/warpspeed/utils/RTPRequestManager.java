package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import uk.co.finleyofthewoods.warpspeed.config.ConfigManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class RTPRequestManager {
    private static final int RTP_REQUEST_COOLDOWN = ConfigManager.get().getRtpCooldown();
    private static final Map<UUID, Integer> rtpCooldowns = new HashMap<>();
    private static final Random RANDOM = new Random();

    public static BlockPos requestRTP(ServerPlayerEntity player, World world) {
        WorldBorder border = world.getWorldBorder();

        double centerX = border.getCenterX();
        double centerZ = border.getCenterZ();
        double size = border.getSize();
        
        double radius = (size / 2) - 16;

        double xOffset = (RANDOM.nextDouble() * 2 - 1) * radius;
        double zOffset = (RANDOM.nextDouble() * 2 - 1) * radius;

        int x = (int) (centerX + xOffset);
        int z = (int) (centerZ + zOffset);

        int y = world.getHeight();
        BlockPos pos = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(pos);
        while (state.isAir()) {
            pos = pos.down();
            state = world.getBlockState(pos);
        }

        player.sendMessage(Text.literal("Teleporting to new location... " + pos.toShortString()), false);

        return pos.up();
    }

    public static int hasRtpRequestCooldownExpired(ServerPlayerEntity player) {
        if (rtpCooldowns.containsKey(player.getUuid())) {
            int timestamp = (int) (System.currentTimeMillis() / 1000);
            int timeSinceLastRequest = timestamp - rtpCooldowns.get(player.getUuid());
            return RTP_REQUEST_COOLDOWN - timeSinceLastRequest;
        }
        return 0;
    }

    public static void setRtpCooldown(ServerPlayerEntity player) {
        rtpCooldowns.put(player.getUuid(), (int) (System.currentTimeMillis() / 1000));
    }
}
