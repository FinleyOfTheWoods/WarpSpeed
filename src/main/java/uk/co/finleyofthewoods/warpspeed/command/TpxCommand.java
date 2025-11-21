package uk.co.finleyofthewoods.warpspeed.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.utils.DatabaseManager;
import uk.co.finleyofthewoods.warpspeed.utils.HomePosition;
import uk.co.finleyofthewoods.warpspeed.utils.TeleportUtils;
import uk.co.finleyofthewoods.warpspeed.utils.tpa.TpxRequestManager;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class TpxCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(TpxCommand.class);

    private static SuggestionProvider<ServerCommandSource> makeTargetSuggestions() {
        return (context, builder) -> {
            try {
                ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                List<ServerPlayerEntity> playerList = context.getSource().getWorld().getPlayers();
                return suggestMatching(playerList, player, builder);
            } catch (CommandSyntaxException e) {
                return Suggestions.empty();
            } catch (Exception e) {
                LOGGER.error("Unexpected Exception: Failed to execute /tpa command", e);
                return Suggestions.empty();
            }
        };
    }

    /**
     * Helper method to filter and suggest matching strings
     */
    private static CompletableFuture<Suggestions> suggestMatching(List<ServerPlayerEntity> candidates, ServerPlayerEntity sender, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        for (ServerPlayerEntity candidate : candidates) {

            if (!candidate.isInvisibleTo(sender) && candidate.getName() != null && String.valueOf(candidate.getName().getString()).startsWith(remaining)) {
                builder.suggest(String.valueOf(candidate.getName().getString()));
            }
        }

        return builder.buildFuture();
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(literal("tpa")
                .requires(source -> source.getPlayer() != null) //todo: permission
                .then(argument("target", StringArgumentType.word())
                        .suggests(makeTargetSuggestions())
                        .executes(TpxCommand::executeTpaRequest))
        );
        dispatcher.register(literal("tpdeny")
                .requires(source -> source.getPlayer() != null) //todo: permission
                        .executes(TpxCommand::executeTpaDenyRequestWithoutTarget)
                            .then(argument("target", StringArgumentType.word()) //optional parameter
                                    .suggests(makeTargetSuggestions())
                                    .executes(TpxCommand::executeTpaDenyRequestWithTarget))
        );

    }

   private static int executeTpaRequest(CommandContext<ServerCommandSource> context) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String targetPlayerName = StringArgumentType.getString(context, "target");

            ServerPlayerEntity target = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName)).getFirst();

            if (player.getUuid().equals(target.getUuid())) {
                player.sendMessage(Text.literal("Can't teleport to yourself."), false);
                return 0;
            }

            boolean success = TpxRequestManager.makeTpaRequest(player, target); // todo: implement me

            if (success) {
                player.sendMessage(Text.literal("Sent teleport request to: " + targetPlayerName), false);
                target.sendMessage(Text.literal(player.getName().getString() + " wants to teleport to your location. Accept with /tpaaccept or /tpaaccept <mcusername> or deny with /tpadeny."));

                //for some fucking reason player and target are swapped... this will play the sound on the target's side
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 0.7f );
                return 1;
            } else {
                return 0;
            }
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to execute /tpa command", e);
            return 0;

        } catch (NoSuchElementException e) {
            LOGGER.error("Failed to find target player", e);
            context.getSource().sendMessage(Text.literal("Couldn't find that player."));

            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpa command", e);
            return 0;
        }
   }


    private static int executeTpaDenyRequestWithTarget(CommandContext<ServerCommandSource> context) {
        String targetPlayerName = StringArgumentType.getString(context, "target");

        Optional<ServerPlayerEntity> target = Optional.empty();
        List<ServerPlayerEntity> foundTargetPlayer = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName));
        if (foundTargetPlayer.isEmpty()) {
            target = Optional.empty();
        } else {
            target = Optional.of(foundTargetPlayer.getFirst());
        }
        return executeTpaDenyRequest(context, target);
    }
    private static int executeTpaDenyRequestWithoutTarget(CommandContext<ServerCommandSource> context) {
        return executeTpaDenyRequest(context, Optional.empty());
    }

    private static int executeTpaDenyRequest(CommandContext<ServerCommandSource> context, Optional<ServerPlayerEntity> target) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            if (target.isPresent() && player.getUuid().equals(target.get().getUuid())) {
                player.sendMessage(Text.literal("<Server>: Aw! You might deny yourself, but I'm never gonna give you up, never gonna let you down,"), false);
                player.sendMessage(Text.literal("Never gonna run around and desert you."), false);
                player.sendMessage(Text.literal("Never gonna make you cry, never gonna say goodbye" ), false);
                player.sendMessage(Text.literal("Never gonna tell a lie and hurt you."), false);
                return 1;
            }

            boolean success = TpxRequestManager.denyTpaRequest(player, target); // todo: implement me
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (CommandSyntaxException e) {
            LOGGER.error("Failed to execute /tpdeny command", e);
            return 0;

        } catch (NoSuchElementException e) {
            LOGGER.error("Failed to find target player", e);
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpdeny command", e);
            return 0;
        }
    }


//   private static int executeSetHome(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
//        try {
//            ServerCommandSource source = context.getSource();
//            ServerPlayerEntity player = source.getPlayerOrThrow();
//            String homeName = StringArgumentType.getString(context, "homeName");
//
//            // check if a home already exists with this name
//            boolean homeExists = dbManager.homeExists(player.getUuid(), homeName);
//            if (homeExists) {
//                player.sendMessage(Text.literal("Home '" + homeName + "' already exists"), false);
//                return 0;
//            }
//
//            // check home limit
//            int homeCount = dbManager.getHomeCount(player.getUuid());
//            if (homeCount >= MAX_HOME_PER_PLAYER) {
//                player.sendMessage(Text.literal("You have reached the maximum number of homes (" + MAX_HOME_PER_PLAYER + ")"), false);
//                return 0;
//            }
//
//            BlockPos pos = player.getBlockPos();
//            World world = player.getEntityWorld();
//            String worldId = world.getRegistryKey().getValue().toString();
//
//            HomePosition home = new HomePosition(player.getUuid(), homeName, worldId, pos.getX(), pos.getY(), pos.getZ());
//            boolean success = dbManager.saveHome(home);
//            if (success) {
//                player.sendMessage(Text.literal("Home '" + homeName + "' set at " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()), false);
//                return 1;
//            } else {
//                player.sendMessage(Text.literal("Failed to set home '" + homeName + "'"), false);
//                return 0;
//            }
//        } catch (Exception e) {
//            LOGGER.error("Unexpected Exception: Failed to execute /setHome command", e);
//            return 0;
//        }
//    }
//
//    private static int executeDeleteHome(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
//        try {
//            ServerCommandSource source = context.getSource();
//            ServerPlayerEntity player = source.getPlayerOrThrow();
//            String homeName = StringArgumentType.getString(context, "homeName");
//
//            boolean success = dbManager.removeHome(player.getUuid(), homeName);
//
//            if (success) {
//                player.sendMessage(Text.literal("Home '" + homeName + "' deleted"), false);
//                return 1;
//            } else {
//                player.sendMessage(Text.literal("Failed to delete home '" + homeName + "'"), false);
//                return 0;
//            }
//        } catch(CommandSyntaxException e) {
//            LOGGER.error("Failed to execute /delHome command", e);
//            return 0;
//        } catch (Exception e) {
//            LOGGER.error("Unexpected Exception: Failed to execute /delHome command", e);
//            return 0;
//        }
//    }
//
//    private static int executeListHomes(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
//        try {
//            ServerCommandSource source = context.getSource();
//            ServerPlayerEntity player = source.getPlayerOrThrow();
//            List<HomePosition> homes = dbManager.getPlayerHomes(player.getUuid());
//            if (homes.isEmpty()) {
//                player.sendMessage(Text.literal("You have no homes set"), false);
//                return 0;
//            }
//
//            player.sendMessage(Text.literal("Your homes:"), false);
//            for (HomePosition home : homes) {
//                player.sendMessage(Text.literal("    " + home.getHomeName() + " at " + home.getX() + ", " + home.getY() + ", " + home.getZ()), false);
//            }
//
//            return 1;
//        } catch(CommandSyntaxException e) {
//            LOGGER.error("Failed to execute /deleteWarp command", e);
//            return 0;
//        } catch (Exception e) {
//            LOGGER.error("Unexpected Exception: Failed to execute /deleteWarp command", e);
//            return 0;
//        }
//    }
}
