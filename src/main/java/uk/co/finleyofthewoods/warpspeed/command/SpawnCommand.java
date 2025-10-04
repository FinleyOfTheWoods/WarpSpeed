package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;

import static net.minecraft.server.command.CommandManager.literal;

public class SpawnCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger("SpawnCommand");

    /**
     * Registers the /spawn command with the command dispatcher.
     *
     * @param dispatcher The command dispatcher to register the command with.
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("spawn")
                .requires(source -> source.getPlayer() != null)
                .executes(SpawnCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            World world = player.getEntityWorld();
            BlockPos spawnPos = world.getSpawnPoint().getPos();
            LOGGER.debug("Player {} executed /spawn command. Teleporting to spawn at {} {} {}",
                player.getName().getString(),
                spawnPos.getX(),
                spawnPos.getY(),
                spawnPos.getZ()
            );
            boolean success = TeleportUtils.teleportToSpawn(player, world, spawnPos);
        } catch (Exception e) {
            LOGGER.error("Failed to execute /spawn command", e);
        }
        return 0;
    }
}
