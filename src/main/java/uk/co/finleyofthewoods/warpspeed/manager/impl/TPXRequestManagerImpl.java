package uk.co.finleyofthewoods.warpspeed.manager.impl;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import uk.co.finleyofthewoods.warpspeed.model.TPARequest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class TPXRequestManagerImpl implements uk.co.finleyofthewoods.warpspeed.manager.TPXRequestManager {
    private static final Long REQUEST_EXPIRY_TIME = 180000L; // 3 minutes
    private static final ConcurrentHashMap<UUID, TPARequest> requests = new ConcurrentHashMap<>();

    @Override
    public void createRequest(@NonNull TPARequest request) {
        log.debug("Adding TPA request for player {} to {}", request.getPlayer().getPlainTextName(), request.getTarget().getPlainTextName());
        requests.put(request.getPlayer().getUUID(), request);
        sendRequest(request.getTarget());
    }

    @Override
    public @Nullable TPARequest getRequest(@NonNull ServerPlayer player, @NonNull ServerPlayer target) {
        log.debug("Getting TPA request for player {}", player.getPlainTextName());
        List<TPARequest> requests = getRequests(player);
        if (requests == null || requests.isEmpty()) {
            return null;
        }
        return requests.stream().filter(r -> r.getTarget().getUUID().equals(target.getUUID())).findFirst().orElse(null);
    }

    @Override
    public @Nullable List<TPARequest> getRequests(@NonNull ServerPlayer player) {
        return requests.values().stream().filter(r -> r.getPlayer().getUUID().equals(player.getUUID())).toList();
    }

    @Override
    public void removeRequest(@NonNull ServerPlayer player) {
        log.debug("Removing TPA request for player {}", player.getPlainTextName());
        requests.remove(player.getUUID());
    }

    @Override
    public void clearExpiredRequests() {
        requests.forEach((uuid, request) -> {
            if (request.getTime() + REQUEST_EXPIRY_TIME < System.currentTimeMillis()) {
                requests.remove(uuid);
            }
        });
    }

    private void sendRequest(ServerPlayer target) {
        target.sendSystemMessage(Component.literal("TP request received from " + target.getDisplayName().getString())
                .withStyle(ChatFormatting.BLUE));
        target.sendSystemMessage(Component.literal("use /tpaccept to accept the request")
                .withStyle(ChatFormatting.GREEN));
        target.sendSystemMessage(Component.literal("use /tpdeny to deny the request")
                .withStyle(ChatFormatting.RED));
    }
}
