package uk.co.finleyofthewoods.warpspeed.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.server.level.ServerPlayer;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TPARequest {
    private ServerPlayer player;
    private ServerPlayer target;
    long time;
}
