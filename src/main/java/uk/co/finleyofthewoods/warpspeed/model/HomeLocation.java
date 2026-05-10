package uk.co.finleyofthewoods.warpspeed.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import uk.co.finleyofthewoods.warpspeed.exception.LocationLevelException;

import java.sql.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class HomeLocation extends BaseLocation {
    private String name;
    public HomeLocation(UUID uuid, String name, BlockPos pos, ServerLevel level) {
        this.name = name;
        super(uuid, pos, level);
    }

    public static HomeLocation getFromResultSet(ResultSet rs, ServerLevel serverLevel) throws SQLException, LocationLevelException {
        UUID uuid = UUID.fromString(rs.getString("player_uuid"));
        String name = rs.getString("home_name");
        String level = rs.getString("world_id");
        int x = rs.getInt("x");
        int y = rs.getInt("y");
        int z = rs.getInt("z");

        MinecraftServer server = serverLevel.getServer();
        ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(level));
        ServerLevel homeLevel = server.getLevel(levelKey);
        if (homeLevel == null) {
            throw new LocationLevelException("Home level not found");
        }
        return new HomeLocation(uuid, name, new BlockPos(x, y, z), homeLevel);
    }
}
