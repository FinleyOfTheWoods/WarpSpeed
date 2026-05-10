package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import uk.co.finleyofthewoods.warpspeed.command.utils.ContextHelper;
import uk.co.finleyofthewoods.warpspeed.exception.MissingSourceException;
import uk.co.finleyofthewoods.warpspeed.manager.TeleportManager;
import uk.co.finleyofthewoods.warpspeed.manager.impl.LocationManagerImpl;
import uk.co.finleyofthewoods.warpspeed.manager.impl.TeleportManagerImpl;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Slf4j
public class WarpCommand {
    private static final TeleportManager teleportManager = new TeleportManagerImpl();
    private static final LocationManagerImpl locationManager = new LocationManagerImpl();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("warp")
                .then(literal("teleport")
                        .then(argument("name", StringArgumentType.string())
                        .executes(WarpCommand::executeTeleportToWarp)))
                .then(literal("set")
                        .then(argument("name", StringArgumentType.string())
                                .then(argument("private", BoolArgumentType.bool())
                                        .executes(WarpCommand::executeSetWarp))))
                .then(literal("delete")
                        .then(argument("name", StringArgumentType.string())
                        .executes(WarpCommand::executeDeleteWarp)))
        );
    }

    private static int executeTeleportToWarp(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = ContextHelper.getSourceFromContext(context);
            ServerPlayer player = ContextHelper.getPlayerFromContext(source);
            if (player == null) {
                source.sendFailure(Component.literal("Unable to get player from context")
                        .withStyle(ChatFormatting.RED));
                return 1;
            }
            String name = ContextHelper.getStringFromContext(context, "name");
            if (name == null || name.isEmpty()) {
                player.sendSystemMessage(Component.literal("Warp name cannot be empty")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            if (teleportManager.teleportWarp(player, name)) {
                player.sendSystemMessage(Component.literal("Teleported to warp " + name)
                        .withStyle(ChatFormatting.GREEN), true);
                return 0;
            } else {
                player.sendSystemMessage(Component.literal("Failed to teleport to warp " + name)
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
        } catch (MissingSourceException e) {
            log.error("Unable to get CommandSourceStack from CommandContext", e);
            return 1;
        } catch (CommandSyntaxException e) {
            log.error("Unable to get ServerPlayer from CommandContext", e);
            return 1;
        } catch (Exception e) {
            log.error("Unexpected error during executeTeleportToWarp", e);
            return 1;
        }
    }

    private static int executeSetWarp(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = ContextHelper.getSourceFromContext(context);
            ServerPlayer player = ContextHelper.getPlayerFromContext(source);
            if (player == null) {
                source.sendFailure(Component.literal("Unable to get player from context")
                        .withStyle(ChatFormatting.RED));
                return 1;
            }
            String name = ContextHelper.getStringFromContext(context, "name");
            if (name == null || name.isEmpty()) {
                player.sendSystemMessage(Component.literal("Warp name cannot be empty")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            boolean isPrivate = ContextHelper.getBooleanFromContext(context, "private");
            if (locationManager.insertWarpLocation(player, name, isPrivate)) {
                player.sendSystemMessage(Component.literal("Warp " + name + " created")
                        .withStyle(ChatFormatting.GREEN), true);
                return 0;
            } else {
                player.sendSystemMessage(Component.literal("Failed to create warp " + name)
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
        } catch (MissingSourceException e) {
            log.error("Unable to get CommandSourceStack from CommandContext", e);
            return 1;
        } catch (CommandSyntaxException e) {
            log.error("Unable to get ServerPlayer from CommandContext", e);
            return 1;
        } catch (Exception e) {
            log.error("Unexpected error during executeSetWarp", e);
            return 1;
        }
    }

    private static int executeDeleteWarp(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = ContextHelper.getSourceFromContext(context);
            ServerPlayer player = ContextHelper.getPlayerFromContext(source);
            if (player == null) {
                source.sendFailure(Component.literal("Unable to get player from context")
                        .withStyle(ChatFormatting.RED));
                return 1;
            }
            String name = ContextHelper.getStringFromContext(context, "name");
            if (name == null || name.isEmpty()) {
                player.sendSystemMessage(Component.literal("Warp name cannot be empty")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            if (locationManager.deleteWarpLocation(player, name)) {
                player.sendSystemMessage(Component.literal("Deleted warp " + name)
                        .withStyle(ChatFormatting.GREEN), true);
                return 0;
            } else {
                player.sendSystemMessage(Component.literal("Failed to delete warp " + name)
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
        } catch (MissingSourceException e) {
            log.error("Unable to get CommandSourceStack from CommandContext", e);
            return 1;
        } catch (CommandSyntaxException e) {
            log.error("Unable to get ServerPlayer from CommandContext", e);
            return 1;
        } catch (Exception e) {
            log.error("Unexpected error during executeDeleteWarp", e);
            return 1;
        }
    }
}
