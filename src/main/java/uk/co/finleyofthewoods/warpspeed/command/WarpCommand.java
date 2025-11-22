package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.utils.DatabaseManager;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;
import uk.co.finleyofthewoods.warpspeed.infrastructure.WarpPosition;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class WarpCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(WarpCommand.class);
    private static final int MAX_WARP_PER_PLAYER = 254;

    private static SuggestionProvider<ServerCommandSource> accessibleWarpSuggestions(DatabaseManager dbManager) {
        return (context, builder) -> {
            try {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                List<String> warpNames = dbManager.getAccessibleWarpNames(player.getUuid());
                return suggestMatching(warpNames, builder);
            } catch(CommandSyntaxException e) {
                return Suggestions.empty();
            }
        };
    }

    private static SuggestionProvider<ServerCommandSource> playerWarpSuggestions(DatabaseManager dbManager) {
        return (context, builder) -> {
            try {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                List<String> warpNames = dbManager.getPlayerWarpNames(player.getUuid());
                return suggestMatching(warpNames, builder);
            } catch(CommandSyntaxException e) {
                return Suggestions.empty();
            }
        };
    }

    private static SuggestionProvider<ServerCommandSource> isPrivateSuggestion() {
        return (context, builder) -> {
            return suggestMatching(Arrays.asList("false", "true"), builder);
        };
    }

    /**
     * Helper method to filter and suggest matching strings
     */
    private static CompletableFuture<Suggestions> suggestMatching(List<String> candidates, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        for (String candidate : candidates) {
            if (candidate.toLowerCase().startsWith(remaining)) {
                builder.suggest(candidate);
            }
        }

        return builder.buildFuture();
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, DatabaseManager dbManager) {
        dispatcher.register(literal("warp")
                .requires(source -> source.getPlayer() != null)
                .then(argument("warpName", StringArgumentType.word())
                        .suggests(accessibleWarpSuggestions(dbManager))
                        .executes(context -> executeTeleportToWarp(context, dbManager)))
        );
        dispatcher.register(literal("setWarp")
                .requires(source -> source.getPlayer() != null)
                .then(argument("warpName", StringArgumentType.word())
                        .then(argument("private", StringArgumentType.word())
                                .suggests(isPrivateSuggestion())
                                .executes(context -> executeSetWarp(context, dbManager))))
        );
        dispatcher.register(literal("delWarp")
                .requires(source -> source.getPlayer() != null)
                .then(argument("warpName", StringArgumentType.word())
                        .suggests(playerWarpSuggestions(dbManager))
                        .executes(context -> executeDeleteWarp(context, dbManager)))
        );
    }

    private static int executeTeleportToWarp(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String warpName = StringArgumentType.getString(context, "warpName");
            World world = player.getEntityWorld();

            boolean success = TeleportUtils.teleportToWarp(player, warpName, dbManager);
            if (success) {
                player.sendMessage(Text.literal("Teleported to warp: " + warpName), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to teleport to warp: " + warpName), false);
                return 0;
            }
        } catch(CommandSyntaxException e) {
            LOGGER.error("Failed to execute /warp command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /warp command", e);
            return 0;
        }
    }

    private static int executeSetWarp(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String warpName = StringArgumentType.getString(context, "warpName");

            if (warpName.length() > 32 || !warpName.matches("[a-zA-Z0-9_-]+")) {
                player.sendMessage(Text.literal("Warp name must be 1-32 alphanumeric characters"), false);
                return 0;
            }

            boolean privateFlag = StringArgumentType.getString(context, "private").equals("true");
            boolean warpExists = dbManager.warpExists(warpName);
            if (warpExists) {
                player.sendMessage(Text.literal("Warp '" + warpName + "' already exists"), false);
                return 0;
            }
            int warpCount = dbManager.getWarpCount(player.getUuid());
            if (warpCount >= MAX_WARP_PER_PLAYER) {
                player.sendMessage(Text.literal("You have reached the maximum number of warps (" + MAX_WARP_PER_PLAYER + ")"), false);
                return 0;
            }

            BlockPos pos = player.getBlockPos();
            World world = player.getEntityWorld();
            String worldId = world.getRegistryKey().getValue().toString();

            WarpPosition warp = new WarpPosition(player.getUuid(), warpName, worldId, privateFlag, pos.getX(), pos.getY(), pos.getZ());
            boolean success = dbManager.saveWarp(warp);
            if (success) {
                player.sendMessage(Text.literal("Warp '" + warpName + "' set at (" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")"), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to set warp '" + warpName + "'"), false);
                return 0;
            }
        } catch(CommandSyntaxException e) {
            LOGGER.error("Failed to execute /setWarp command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /setWarp command", e);
            return 0;
        }
    }
    private static int executeDeleteWarp(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String warpName = StringArgumentType.getString(context, "warpName");

            boolean warpExists = dbManager.warpExists(warpName);
            if (!warpExists) {
                player.sendMessage(Text.literal("Warp '" + warpName + "' does not exist"), false);
                return 0;
            }

            boolean success = dbManager.removeWarp(player.getUuid(), warpName);
            if (success) {
                player.sendMessage(Text.literal("Warp '" + warpName + "' deleted"), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to delete warp '" + warpName + "'"), false);
                return 0;
            }
        } catch(CommandSyntaxException e) {
            LOGGER.error("Failed to execute /deleteWarp command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /deleteWarp command", e);
            return 0;
        }
    }
}
