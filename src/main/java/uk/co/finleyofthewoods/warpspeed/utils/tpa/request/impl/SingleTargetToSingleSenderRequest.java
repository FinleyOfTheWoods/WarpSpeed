package uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.*;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.AbstractTpxRequest;

import java.util.List;

public class SingleTargetToSingleSenderRequest extends AbstractTpxRequest {

    public SingleTargetToSingleSenderRequest(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        super(sender, List.of(receiver), TpxDirection.RECEIVER_TO_SENDER, true, List.of()); //todo: permission
    }

    @Override
    public boolean canTeleport() {
        return isActive();
    }

    @Override
    public boolean doTeleport() throws TpxExpiredException {
        if (!isActive())
            throw new TpxExpiredException("§c§oRequest has expired.");

        return TeleportUtils.teleportPlayerToPlayer(getReceivers().getFirst(), getSender());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SingleTargetToSingleSenderRequest that = (SingleTargetToSingleSenderRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! getReceivers().getFirst().getUuid().equals(that.getReceivers().getFirst().getUuid())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
