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
import uk.co.finleyofthewoods.warpspeed.infrastructure.HomePosition;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class HomeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeCommand.class);
    private static final int MAX_HOME_PER_PLAYER = 10;

    private static SuggestionProvider<ServerCommandSource> homeSuggestions(DatabaseManager dbManager) {
        return (context, builder) -> {
            try {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                List<HomePosition> homeNames = dbManager.getPlayerHomes(player.getUuid());
                List<String> homeNamesList = homeNames.stream().map(HomePosition::homeName).toList();
                return suggestMatching(homeNamesList, builder);
            } catch (CommandSyntaxException e) {
                return Suggestions.empty();
            } catch (Exception e) {
                LOGGER.error("Unexpected Exception: Failed to execute /home command", e);
                return Suggestions.empty();
            }
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
        // /home <name> - Teleport to home
        dispatcher.register(literal("home")
                .requires(source -> source.getPlayer() != null)
                .then(argument("homeName", StringArgumentType.word())
                        .suggests(homeSuggestions(dbManager))
                        .executes(context -> executeTeleportHome(context, dbManager)))
        );

        // /homes - List all homes
        dispatcher.register(literal("homes")
                .requires(source -> source.getPlayer() != null)
                .executes(context -> executeListHomes(context, dbManager))
        );

        // /setHome <name> - Set a home
        dispatcher.register(literal("setHome")
                .requires(source -> source.getPlayer() != null)
                .then(argument("homeName", StringArgumentType.word())
                        .executes(context -> executeSetHome(context, dbManager)))
        );

        // /delHome <name> - Delete a home
        dispatcher.register(literal("delHome")
                .requires(source -> source.getPlayer() != null)
                .then(argument("homeName", StringArgumentType.word())
                        .suggests(homeSuggestions(dbManager))
                        .executes(context -> executeDeleteHome(context, dbManager)))
        );
    }

   private static int executeTeleportHome(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String homeName = StringArgumentType.getString(context, "homeName");
            World world = player.getEntityWorld();

            if (homeName.length() > 32 || !homeName.matches("[a-zA-Z0-9_-]+")) {
                player.sendMessage(Text.literal("Home name must be 1-32 alphanumeric characters"), false);
                return 0;
            }

            boolean success = TeleportUtils.teleportToHome(player, homeName, dbManager);
            if (success) {
                player.sendMessage(Text.literal("Teleported to home: " + homeName), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to teleport to home: " + homeName), false);
                return 0;
            }
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to execute /home command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /home command", e);
            return 0;
        }
   }

   private static int executeSetHome(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String homeName = StringArgumentType.getString(context, "homeName");

            // check if a home already exists with this name
            boolean homeExists = dbManager.homeExists(player.getUuid(), homeName);
            if (homeExists) {
                player.sendMessage(Text.literal("Home '" + homeName + "' already exists"), false);
                return 0;
            }

            // check home limit
            int homeCount = dbManager.getHomeCount(player.getUuid());
            if (homeCount >= MAX_HOME_PER_PLAYER) {
                player.sendMessage(Text.literal("You have reached the maximum number of homes (" + MAX_HOME_PER_PLAYER + ")"), false);
                return 0;
            }

            BlockPos pos = player.getBlockPos();
            World world = player.getEntityWorld();
            String worldId = world.getRegistryKey().getValue().toString();

            HomePosition home = new HomePosition(player.getUuid(), homeName, worldId, pos.getX(), pos.getY(), pos.getZ());
            boolean success = dbManager.saveHome(home);
            if (success) {
                player.sendMessage(Text.literal("Home '" + homeName + "' set at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to set home '" + homeName + "'"), false);
                return 0;
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /setHome command", e);
            return 0;
        }
    }

    private static int executeDeleteHome(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String homeName = StringArgumentType.getString(context, "homeName");

            boolean success = dbManager.removeHome(player.getUuid(), homeName);

            if (success) {
                player.sendMessage(Text.literal("Home '" + homeName + "' deleted"), false);
                return 1;
            } else {
                player.sendMessage(Text.literal("Failed to delete home '" + homeName + "'"), false);
                return 0;
            }
        } catch(CommandSyntaxException e) {
            LOGGER.error("Failed to execute /delHome command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /delHome command", e);
            return 0;
        }
    }

    private static int executeListHomes(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            List<HomePosition> homes = dbManager.getPlayerHomes(player.getUuid());
            if (homes.isEmpty()) {
                player.sendMessage(Text.literal("You have no homes set"), false);
                return 0;
            }

            player.sendMessage(Text.literal("Your homes:"), false);
            for (HomePosition home : homes) {
                player.sendMessage(Text.literal("    " + home.homeName() + " at " + home.x() + ", " + home.y() + ", " + home.z()), false);
            }

            return 1;
        } catch(CommandSyntaxException e) {
            LOGGER.error("Failed to execute /deleteWarp command", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /deleteWarp command", e);
            return 0;
        }
    }
}
