package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import uk.co.finleyofthewoods.warpspeed.command.utils.ContextHelper;
import uk.co.finleyofthewoods.warpspeed.manager.TeleportManager;
import uk.co.finleyofthewoods.warpspeed.manager.impl.TeleportManagerImpl;

import static net.minecraft.commands.Commands.literal;

@Slf4j
public class RTPCommand {
    private static final TeleportManager teleportManager = new TeleportManagerImpl();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("rtp")
                .executes(RTPCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = ContextHelper.getSourceFromContext(context);
            ServerPlayer player = ContextHelper.getPlayerFromContext(source);
            if (player == null) {
                source.sendFailure(Component.literal("Unable to get player from context")
                        .withStyle(ChatFormatting.RED));
                return 1;
            }
            if (teleportManager.teleportRandomly(player)) {
                player.sendSystemMessage(Component.literal("Successfully teleported randomly")
                        .withStyle(ChatFormatting.GREEN), true);
            } else {
                player.sendSystemMessage(Component.literal("Failed to teleport randomly")
                        .withStyle(ChatFormatting.RED), true);
            }
            return 0;
        } catch (Exception e) {
            log.error("Failed to execute rtp command", e);
            return 1;
        }
    }
}
