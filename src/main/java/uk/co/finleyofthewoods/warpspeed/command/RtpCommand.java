package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;

import static net.minecraft.server.command.CommandManager.literal;

public class RtpCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpCommand.class);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("rtp")
                .requires(source -> source.getPlayer() != null)
                .executes(RtpCommand::execute)
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            World world = player.getEntityWorld();
            boolean success = TeleportUtils.teleportToRandomLocation(player, world);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch(CommandSyntaxException e) {
            LOGGER.error("Failed to execute /rtp command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /rtp command", e);
            return 0;
        }
    }
}
