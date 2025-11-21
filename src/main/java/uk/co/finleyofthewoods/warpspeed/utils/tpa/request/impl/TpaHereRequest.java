package uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import org.apache.commons.lang3.NotImplementedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDeniedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxExpiredException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxNotAllowedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.BasicTpxHereRequest;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.BasicTpxRequest;

import java.util.List;

public class TpaHereRequest extends BasicTpxHereRequest{

    public TpaHereRequest(ServerPlayerEntity sender, ServerPlayerEntity receiver) {
        super(sender, List.of(receiver), true, List.of()); //todo: permission
    }

    @Override
    public void teleport() throws TpxNotAllowedException, TpxDeniedException, TpxExpiredException {
        //todo: implement with TeleportUtils
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TpaHereRequest that = (TpaHereRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! getReceivers().getFirst().getUuid().equals(that.getReceivers().getFirst().getUuid())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
