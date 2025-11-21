package uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDeniedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxExpiredException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxNotAllowedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.BasicTpxHereRequest;

import java.util.HashSet;
import java.util.List;

public class TpAllHereRequest extends BasicTpxHereRequest {

    public TpAllHereRequest(ServerPlayerEntity sender) {
        super(sender, List.of(), false, List.of()); //todo: permission,  get All Players and return
    }

    @Override
    public void teleport() throws TpxNotAllowedException, TpxDeniedException, TpxExpiredException {
        //todo: implement with TeleportUtils
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TpAllHereRequest that = (TpAllHereRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! new HashSet<>(getReceivers().stream().map(ServerPlayerEntity::getUuid).toList())
                .containsAll(that.getReceivers().stream().map(ServerPlayerEntity::getUuid).toList())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
