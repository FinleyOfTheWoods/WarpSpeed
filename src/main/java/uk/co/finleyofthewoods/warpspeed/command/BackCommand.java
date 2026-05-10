package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import uk.co.finleyofthewoods.warpspeed.command.utils.ContextHelper;
import uk.co.finleyofthewoods.warpspeed.manager.TeleportManager;
import uk.co.finleyofthewoods.warpspeed.manager.impl.LocationManagerImpl;
import uk.co.finleyofthewoods.warpspeed.manager.impl.TeleportManagerImpl;

import static net.minecraft.commands.Commands.literal;

@Slf4j
public class BackCommand {
    private static final TeleportManager teleportManager = new TeleportManagerImpl();
    private static final LocationManagerImpl locationManager = new LocationManagerImpl();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("back")
                .executes(BackCommand::execute));
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
            BlockPos currentPos = player.getOnPos();
            if (teleportManager.teleportBack(player)) {
                locationManager.setPreviousLocation(player, currentPos);
                player.sendSystemMessage(Component.literal("Teleported to previous location")
                        .withStyle(ChatFormatting.GREEN), true);
                return 0;
            } else {
                player.sendSystemMessage(Component.literal("Failed to teleport to previous location")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
        } catch (Exception e) {
            log.error("Failed to execute back command", e);
            return 1;
        }
    }
}
