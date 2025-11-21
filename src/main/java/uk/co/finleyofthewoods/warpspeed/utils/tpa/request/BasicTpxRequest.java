package uk.co.finleyofthewoods.warpspeed.utils.tpa.request;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpaStatus;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDirection;

import java.util.List;

public abstract class BasicTpxRequest extends AbstractTpxRequest {

    public BasicTpxRequest(ServerPlayerEntity sender, List<ServerPlayerEntity> receiver, boolean needsApproval, List<String> necessaryPermissions) {
        super(sender, receiver, TpxDirection.SENDER_TO_RECEIVER, needsApproval, necessaryPermissions); // todo: permissions
    }

    @Override
    public boolean canTeleport() {
        if (needsApproval()) {
            return isActive() && getStatus() != TpaStatus.ACCEPTED && hasPermission();
        } else {
            return true;
        }
    }

    public boolean needsApproval(){
        return needsApproval;
    }

    public abstract void teleport();


    @Override
    public abstract boolean equals(Object obj);
}
