package uk.co.finleyofthewoods.warpspeed.utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public record BlocklistOfPlayer(String blockerPlayerUserName, Map<String, Long> blockedByBlockerAt) {

    public String getBlocker() {
        return blockerPlayerUserName;
    }

    public Map<String, Long> getBlockedByPlayer() {
        return blockedByBlockerAt;
    }

    @Override
    public @NotNull String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BlocklistOfPlayer{player=").append(blockerPlayerUserName).append(", isBlocking=[");
        List<String> blockedPlayers = blockedByBlockerAt.keySet().stream()
                .toList();
        sb.append(String.join(", ", blockedPlayers));
        sb.append("]}");
        return sb.toString();
    }

    //build a json of this
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"player\": \"").append(blockerPlayerUserName).append("\", \"isBlocking\": [");
        List<String> blockedPlayers = blockedByBlockerAt.keySet().stream()
                .map(username -> "\"" + username + "\"")
                .toList();
        sb.append(String.join(", ", blockedPlayers));
        sb.append("]}");
        return sb.toString();
    }
}
