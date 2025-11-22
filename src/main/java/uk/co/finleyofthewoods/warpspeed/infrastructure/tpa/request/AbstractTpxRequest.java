package uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.TpxDirection;

import java.time.Instant;
import java.util.List;

public abstract class AbstractTpxRequest {
    Instant requestTimestamp;
    ServerPlayerEntity sender;
    List<ServerPlayerEntity> receivers;
    TpxDirection direction;
    List<String> necessaryPermissions;
    boolean needsApproval;

    public AbstractTpxRequest(ServerPlayerEntity sender, List<ServerPlayerEntity> receivers, TpxDirection direction, boolean needsApproval, List<String> necessaryPermissions ) {
        setSender(sender);
        setReceivers(receivers);
        setDirection(direction);
        this.necessaryPermissions = necessaryPermissions;
        this.requestTimestamp = Instant.now();
        this.needsApproval = needsApproval;
    }

    public ServerPlayerEntity getSender() {
        return sender;
    }

    public List<ServerPlayerEntity> getReceivers() {
        return receivers;
    }

    public TpxDirection getDirection() {
        return direction;
    }

    public void setSender(ServerPlayerEntity sender) {
        this.sender = sender;
    }

    public void setReceivers(List<ServerPlayerEntity> receivers) {
        this.receivers = receivers;
    }

    public void setDirection(TpxDirection direction) {
        this.direction = direction;
    }

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public boolean isActive() {
        Instant now = Instant.now();
        //todo: expiration time configurable
        long elapsedSeconds = now.getEpochSecond() - requestTimestamp.getEpochSecond();
        // 20 seconds
        return elapsedSeconds <= 20;
    }

    public boolean hasPermission() {
        //todo : implement permission checking logic
        return true;
    }

    public abstract boolean canTeleport();

    public abstract boolean doTeleport();

    public abstract boolean equals(Object obj);

    public boolean needsApproval() {
        return needsApproval;
    }



//ideas: List of receivers instead of single receiver, wouldn't need to be accepted since it's permission only

}
