package uk.co.finleyofthewoods.warpspeed.utils.tpa.request.impl;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDirection;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxExpiredException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxNotAllowedException;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.request.AbstractTpxRequest;

import java.util.HashSet;
import java.util.List;

public class MultipleTargetsToPrivilegedSenderRequest extends AbstractTpxRequest {

    public MultipleTargetsToPrivilegedSenderRequest(ServerPlayerEntity sender) {
        super(sender, List.of(), TpxDirection.RECEIVER_TO_SENDER, false, List.of()); //todo: permission,  get All Players and return
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

        MultipleTargetsToPrivilegedSenderRequest that = (MultipleTargetsToPrivilegedSenderRequest) obj;

        if (!getSender().getUuid().equals(that.getSender().getUuid())) return false;
        if (! new HashSet<>(getReceivers().stream().map(ServerPlayerEntity::getUuid).toList())
                .containsAll(that.getReceivers().stream().map(ServerPlayerEntity::getUuid).toList())) return false;
        return getRequestTimestamp().equals(that.getRequestTimestamp());
    }
}
