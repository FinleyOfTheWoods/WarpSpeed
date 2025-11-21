package uk.co.finleyofthewoods.warpspeed.utils.tpa.request;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpaStatus;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDirection;

import java.time.Instant;
import java.util.List;

public interface TpxRequestable {

    ServerPlayerEntity getSender();

    List<ServerPlayerEntity> getReceivers();

    TpxDirection getDirection();
    void setSender(ServerPlayerEntity sender);
    void setReceivers(List<ServerPlayerEntity> receiver);
    void setDirection(TpxDirection direction);

    Instant getRequestTimestamp();
    void teleport();

    void setStatus(TpaStatus status);
    boolean canTeleport();
    boolean needsApproval();
    TpaStatus getStatus();
    boolean isActive();
    boolean hasPermission();

}
