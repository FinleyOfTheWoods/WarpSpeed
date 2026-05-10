package uk.co.finleyofthewoods.warpspeed.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarpLocation extends BaseLocation {
    private String name;
    private boolean isPrivate;

    public WarpLocation(UUID uuid, String name, boolean isPrivate, BlockPos pos, ServerLevel level) {
        this.name = name;
        this.isPrivate = isPrivate;
        super(uuid, pos, level);
    }
}
