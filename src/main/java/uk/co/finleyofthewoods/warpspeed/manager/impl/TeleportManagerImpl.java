package uk.co.finleyofthewoods.warpspeed.manager.impl;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import uk.co.finleyofthewoods.warpspeed.exception.TeleportFailureException;
import uk.co.finleyofthewoods.warpspeed.manager.TeleportManager;
import uk.co.finleyofthewoods.warpspeed.model.BaseLocation;
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;
import uk.co.finleyofthewoods.warpspeed.model.WarpLocation;

@Slf4j
public class TeleportManagerImpl implements TeleportManager {
    private static final LocationManagerImpl locationManager = new LocationManagerImpl();

    private boolean teleport(@NonNull ServerPlayer player, @NonNull HomeLocation location) {
        try {
            log.debug("Teleporting {} to home {} ({})", player.getDisplayName(), location.getName(), location.getPos());
            ServerLevel level = player.level();
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
            ServerLevel level = player.level();
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
            ServerLevel level = player.level();
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

    @Override
    public boolean teleportHome(@NonNull ServerPlayer player, @NonNull String name) {
        HomeLocation location = locationManager.getHomeLocationByName(player, name);
        if (location == null) {
            log.debug("Home location {} not found for player {}", name, player.getPlainTextName());
            player.sendSystemMessage(Component.literal("Home location not found").withStyle(ChatFormatting.RED), true);
            return false;
        }
        return teleport(player, location);
    }
}
