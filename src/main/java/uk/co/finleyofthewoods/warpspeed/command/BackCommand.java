package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.utils.PlayerLocationTracker;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;

public class BackCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackCommand.class);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("back").executes(BackCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            if (!source.isExecutedByPlayer()) {
                LOGGER.warn("Command executed by non-player");
                source.sendError(Text.literal("This command can only be executed by a player"));
                return 0;
            }
            ServerPlayerEntity player = source.getPlayerOrThrow();
            if (!PlayerLocationTracker.hasPreviousLocation(player)) {
                player.sendMessage(Text.literal("No previous location found"), false);
                return 0;
            }
            boolean success = TeleportUtils.teleportToLastLocation(player);
            if (success) {
                player.sendMessage(Text.literal("Teleported to previous location"), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to teleport to previous location"), false);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /back command", e);
            return 0;
        }
    }
}
