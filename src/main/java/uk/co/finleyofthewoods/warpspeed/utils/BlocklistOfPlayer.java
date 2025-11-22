package uk.co.finleyofthewoods.warpspeed.utils;

import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record BlocklistOfPlayer(UUID blockerPlayerUUID, Map<UUID, Long> blockedByBlockerAt) {

    public UUID getBlocker() {
        return blockerPlayerUUID;
    }

    public Map<UUID, Long> getBlockedByPlayer() {
        return blockedByBlockerAt;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlocklistOfPlayer{player=").append(blockerPlayerUUID).append(", isBlocking=[");
        List<String> blockedPlayers = blockedByBlockerAt.keySet().stream()
                .map(UUID::toString)
                .toList();
        sb.append(String.join(", ", blockedPlayers));
        sb.append("]}");
        return sb.toString();
    }

    //build a json of this
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"player\": \"").append(blockerPlayerUUID.toString()).append("\", \"isBlocking\": [");
        List<String> blockedPlayers = blockedByBlockerAt.keySet().stream()
                .map(uuid -> "\"" + uuid.toString() + "\"")
                .toList();
        sb.append(String.join(", ", blockedPlayers));
        sb.append("]}");
        return sb.toString();
    }
}
