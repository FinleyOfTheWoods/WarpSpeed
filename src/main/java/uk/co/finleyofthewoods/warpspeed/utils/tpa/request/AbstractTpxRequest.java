package uk.co.finleyofthewoods.warpspeed.utils.tpa.request;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpaStatus;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDirection;

import java.time.Instant;
import java.util.List;

public abstract class AbstractTpxRequest implements TpxRequestable {
    Instant requestTimestamp;
    ServerPlayerEntity sender;
    List<ServerPlayerEntity> receivers;
    TpxDirection direction;
    TpaStatus status;
    List<String> necessaryPermissions;
    boolean needsApproval;

    public AbstractTpxRequest(ServerPlayerEntity sender, List<ServerPlayerEntity> receivers, TpxDirection direction, boolean needsApproval, List<String> necessaryPermissions ) {
        setSender(sender);
        setReceivers(receivers);
        setDirection(direction);
        this.necessaryPermissions = necessaryPermissions;
        this.status = TpaStatus.PENDING;
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
        return TpxDirection.SENDER_TO_RECEIVER;
    }

    public TpaStatus getStatus() {
        return this.status;
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

    public void setStatus(TpaStatus status) {
        this.status = status;
    }

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public boolean isActive() {
        Instant now = Instant.now();
        //todo: make wait time configurable
        long elapsedSeconds = now.getEpochSecond() - requestTimestamp.getEpochSecond();
        if (elapsedSeconds > 120) { // 2 minutes
            setStatus(TpaStatus.EXPIRED);
            return false;
        }
        return true;
    }

    public boolean hasPermission() {
        //todo : implement permission checking logic
        return true;
    }

    public abstract boolean canTeleport();

    public abstract void teleport();

    public abstract boolean equals(Object obj);



//ideas: List of receivers instead of single receiver, wouldn't need to be accepted since it's permission only

}
