package uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDirection;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxExpiredException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxNotAllowedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.AbstractTpxRequest;

import java.util.List;

public class SingleTargetToPrivilegedSenderRequest extends AbstractTpxRequest {

    public SingleTargetToPrivilegedSenderRequest(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        super(sender, List.of(receiver), TpxDirection.RECEIVER_TO_SENDER, false, List.of()); // todo: permissions
    }

    @Override
    public boolean canTeleport() {
        return true; //todo: logic
    }

    @Override
    public boolean doTeleport() throws TpxNotAllowedException, TpxExpiredException {
        //todo: implement with TeleportUtils
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SingleTargetToPrivilegedSenderRequest that = (SingleTargetToPrivilegedSenderRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! getReceivers().getFirst().getUuid().equals(that.getReceivers().getFirst().getUuid())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
