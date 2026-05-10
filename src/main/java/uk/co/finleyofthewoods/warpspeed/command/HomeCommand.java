package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
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
import uk.co.finleyofthewoods.warpspeed.model.HomeLocation;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

@Slf4j
public class HomeCommand {
    private static final TeleportManager teleportManager = new TeleportManagerImpl();
    private static final LocationManagerImpl locationManager = new LocationManagerImpl();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("home")
                    .then(literal("teleport")
                            .then(argument("name", StringArgumentType.string())
                                    .executes(HomeCommand::executeTeleportHome)))
                    .then(literal("set")
                            .then(argument("name", StringArgumentType.string())
                                    .executes(HomeCommand::executeSetHome)))
                    .then(literal("delete")
                            .then(argument("name", StringArgumentType.string())
                                    .executes(HomeCommand::executeDeleteHome)))
                .then(literal("list")
                        .executes(HomeCommand::executeListHomes))
        );
    }

    private static int executeTeleportHome(CommandContext<CommandSourceStack> context) {
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
                player.sendSystemMessage(Component.literal("Home name cannot be empty")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            log.debug("Teleporting player {} to home {}", player.getPlainTextName(), name);
            if (teleportManager.teleportHome(player, name)) {
                player.sendSystemMessage(Component.literal("Teleported to home " + name)
                        .withStyle(ChatFormatting.GREEN), true);
                return 0;
            } else {
                player.sendSystemMessage(Component.literal("Failed to teleport to home " + name)
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
            log.error("Unexpected error during executeTeleportHome", e);
            return 1;
        }
    }

    private static int executeSetHome(CommandContext<CommandSourceStack> context) {
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
                player.sendSystemMessage(Component.literal("Home name cannot be empty")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            boolean created = locationManager.createHomeLocation(player, name);
            if (!created) {
                player.sendSystemMessage(Component.literal("Failed to create home location " + name)
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            player.sendSystemMessage(Component.literal("Created home location " + name)
                    .withStyle(ChatFormatting.GREEN), true);
            return 0;
        } catch (Exception e) {
            return 1;
        }
    }

    private static int executeDeleteHome(CommandContext<CommandSourceStack> context) {
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
                player.sendSystemMessage(Component.literal("Home name cannot be empty")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            boolean deleted = locationManager.deleteHomeLocation(player, name);
            if (deleted) {
                player.sendSystemMessage(Component.literal("Home location not found")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            player.sendSystemMessage(Component.literal("Deleted home location " + name)
                    .withStyle(ChatFormatting.GREEN), true);
            return 0;
        } catch (Exception e) {
            return 1;
        }
    }

    private static int executeListHomes(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = ContextHelper.getSourceFromContext(context);
            ServerPlayer player = ContextHelper.getPlayerFromContext(source);
            if (player == null) {
                source.sendFailure(Component.literal("Unable to get player from context")
                        .withStyle(ChatFormatting.RED));
                return 1;
            }
            List<HomeLocation> homes = locationManager.getHomeLocationsByPlayerId(player);
            if (homes == null || homes.isEmpty()) {
                player.sendSystemMessage(Component.literal("No homes found")
                        .withStyle(ChatFormatting.RED), true);
                return 1;
            }
            for (HomeLocation home : homes) {
                player.sendSystemMessage(Component.literal(home.getName()));
            }
            return 0;
        } catch (Exception e) {
            return 1;
        }
    }
}
