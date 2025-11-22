package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.config.ConfigManager;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarpspeedCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpspeedCommand.class);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("warpspeed")
                .requires(source -> source.hasPermissionLevel(4))
                .then(argument("maxPlayerHomes", IntegerArgumentType.integer(0, 1000))
                        .executes(WarpspeedCommand::executeMaxPlayerHomes))
                .then(argument("maxPlayerWarps", IntegerArgumentType.integer(0, 1000))
                        .executes(WarpspeedCommand::executeMaxPlayerWarps)
                ));
    }

    private static int executeMaxPlayerHomes(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
        try {
            ConfigManager.setMaxPlayerHomes(IntegerArgumentType.getInteger(context, "maxPlayerHomes"));
            player.sendMessage(Text.literal("Max player homes set successfully to: " + IntegerArgumentType.getInteger(context, "maxPlayerWarps")), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error whilst setting max player homes", e);
            player.sendMessage(Text.literal("Unexpected error whilst saving config. Please check logs"), false);
            return 0;
        }
    }

    private static int executeMaxPlayerWarps(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        assert player != null;
        try {
            ConfigManager.setMaxPlayerWarps(IntegerArgumentType.getInteger(context, "maxPlayerHomes"));
            player.sendMessage(Text.literal("Max player warps set successfully to: " + IntegerArgumentType.getInteger(context, "maxPlayerWarps")), false);
            return 1;
        } catch (Exception e) {
            LOGGER.error("Unexpected error whilst setting max player warps", e);
            player.sendMessage(Text.literal("Unexpected error whilst saving config. Please check logs"), false);
            return 0;
        }
    }
}
