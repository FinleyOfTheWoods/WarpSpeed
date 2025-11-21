package uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDeniedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxExpiredException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxNotAllowedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.BasicTpxHereRequest;

import java.util.List;

public class TpHereRequest extends BasicTpxHereRequest{

    public TpHereRequest(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        super(sender, List.of(receiver), false, List.of()); // todo: permissions
    }

    @Override
    public void teleport() throws TpxNotAllowedException, TpxDeniedException, TpxExpiredException {
        //todo: implement with TeleportUtils
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TpHereRequest that = (TpHereRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! getReceivers().getFirst().getUuid().equals(that.getReceivers().getFirst().getUuid())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
