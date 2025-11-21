package uk.co.finleyofthewoods.warpspeed.utils.tpa.request;

import net.minecraft.server.network.ServerPlayerEntity;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpaStatus;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxDirection;

import java.util.List;

public abstract class BasicTpxHereRequest extends AbstractTpxRequest {

    public BasicTpxHereRequest(ServerPlayerEntity sender, List<ServerPlayerEntity> receivers, boolean needsApproval,  List<String> necessaryPermissions) {
        super(sender, receivers, TpxDirection.RECEIVER_TO_SENDER, needsApproval, necessaryPermissions); // todo: permissions
    }

    @Override
    public boolean canTeleport() {
        return isActive() && getStatus() != TpaStatus.ACCEPTED && hasPermission();
    }

    public abstract void teleport();

    @Override
    public abstract boolean equals(Object obj);

    public boolean needsApproval(){
        return needsApproval;
    }

}
