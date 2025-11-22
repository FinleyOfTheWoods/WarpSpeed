package uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions.TpxExpiredException;
import uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions.TpxNotAllowedException;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.*;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.AbstractTpxRequest;

import java.util.List;

public class SenderToSingleTargetRequest extends AbstractTpxRequest {

    public SenderToSingleTargetRequest(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        super(sender, List.of(receiver), TpxDirection.SENDER_TO_RECEIVER, true, List.of()); //todo: permission
    }

    @Override
    public boolean canTeleport() {
        return isActive();
    }

    @Override
    public boolean doTeleport() throws TpxNotAllowedException, TpxExpiredException {
        if (!isActive())
            throw new TpxExpiredException("§c§oRequest has expired.");

        return TeleportUtils.teleportPlayerToPlayer(getSender(), getReceivers().getFirst());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SenderToSingleTargetRequest that = (SenderToSingleTargetRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! getReceivers().getFirst().getUuid().equals(that.getReceivers().getFirst().getUuid())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
