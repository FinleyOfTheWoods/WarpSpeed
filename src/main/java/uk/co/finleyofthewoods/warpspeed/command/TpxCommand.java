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
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.co.finleyofthewoods.warpspeed.infrastructure.exceptions.*;
import uk.co.finleyofthewoods.warpspeed.utils.DatabaseManager;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.impl.MultipleTargetsToPrivilegedSenderRequest;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.impl.SenderToSingleTargetRequest;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.impl.SingleTargetToPrivilegedSenderRequest;
import uk.co.finleyofthewoods.warpspeed.infrastructure.tpa.request.impl.SingleTargetToSingleSenderRequest;
import uk.co.finleyofthewoods.warpspeed.utils.TpxRequestManager;

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

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, DatabaseManager dbManager) {
        dispatcher.register(literal("tpa")
                .requires(source -> source.getPlayer() != null)
                .then(argument("target", StringArgumentType.word())
                        .suggests(makeTargetSuggestions())
                        .executes(context -> executeTpaRequest(context, dbManager)))
        );
        dispatcher.register(literal("tpahere")
                .requires(source -> source.getPlayer() != null)
                .then(argument("target", StringArgumentType.word())
                        .suggests(makeTargetSuggestions())
                        .executes(context -> executeTpaHereRequest(context, dbManager)))
        );
        dispatcher.register(literal("tpdeny")
                .requires(source -> source.getPlayer() != null)
                        .executes(context -> executeTpaDenyRequest(context, false))
                            .then(argument("target", StringArgumentType.word()) //optional parameter
                                    .suggests(makeTargetSuggestions())
                                    .executes(context -> executeTpaDenyRequest(context, true)))
        );
        dispatcher.register(literal("tpaccept")
                .requires(source -> source.getPlayer() != null)
                .executes( context -> executeTpaAcceptRequest(context, false))
                .then(argument("target", StringArgumentType.word()) //optional parameter
                        .suggests(makeTargetSuggestions())
                        .executes(context -> executeTpaAcceptRequest(context, true)))

        );
        dispatcher.register(literal("tpcancel")
                .requires(source -> source.getPlayer() != null)
                .executes( context -> executeTpaCancelRequest(context, false))
                .then(argument("target", StringArgumentType.word()) //optional parameter
                        .suggests(makeTargetSuggestions())
                        .executes(context -> executeTpaCancelRequest(context, true)))
        );
        dispatcher.register(literal("tpblock")
                .requires(source -> source.getPlayer() != null)
                .then(argument("target", StringArgumentType.word())
                        .suggests(makeTargetSuggestions())
                        .executes(context -> executeTpBlock(context, dbManager)))
        );
        dispatcher.register(literal("tpunblock")
                .requires(source -> source.getPlayer() != null)
                .then(argument("target", StringArgumentType.word())
                        .suggests(makeTargetSuggestions())
                        .executes(context -> executeTpUnblock(context, dbManager)))
        );
        dispatcher.register(literal("tpblocklist")
                .requires(source -> source.getPlayer() != null)
                .executes( context -> executeTpBlocklist(context, dbManager))
        );
        /*dispatcher.register(literal("tphere")
                .requires(source -> source.getPlayer() != null) //todo: permission
                .then(argument("target", StringArgumentType.word())
                        .suggests(makeTargetSuggestions())
                        .executes(TpxCommand::executeTpHereRequest))
        );
        dispatcher.register(literal("tphereall")
                .requires(source -> source.getPlayer() != null) //todo: permission
                .executes(TpxCommand::executeTpHereAllRequest)
        );*/
    }

    private static int executeTpaRequest(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String targetPlayerName = StringArgumentType.getString(context, "target");

            ServerPlayerEntity target = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName)).getFirst();

            if (player.getUuid().equals(target.getUuid())) {
                player.sendMessage(Text.literal("§o§cCan't teleport to yourself."), false);
                return 0;
            }
            boolean success = TpxRequestManager.makeSenderToSingleTargetRequest(new SenderToSingleTargetRequest(player, target), dbManager);

            if (success) {
                player.sendMessage(Text.literal("§6Sent teleport request to: " + targetPlayerName), false);
                target.sendMessage(Text.literal("§6" + player.getName().getString() + " wants to teleport to your location. Accept with §6§o/tpaaccept§6 or §6§o/tpaccept <mcusername>§6 or deny with §6§o/tpdeny§6 or §6§o/tpdeny <mcusername>§6."));

                //for some fucking reason player and target are swapped... this will play the sound on the target's side
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 0.7f );
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException  e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpa command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
   }

    private static int executeTpaHereRequest(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String targetPlayerName = StringArgumentType.getString(context, "target");

            ServerPlayerEntity target = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName)).getFirst();

            boolean success = TpxRequestManager.makeSingleTargetToSenderRequest(new SingleTargetToSingleSenderRequest(player, target), dbManager);

            if (success) {
                player.sendMessage(Text.literal("§6Sent teleport request to: " + targetPlayerName), false);
                target.sendMessage(Text.literal("§6"+ player.getName().getString() + " wants you to teleport to their location. Accept with §6§o/tpaaccept§6 or §6§o/tpaaccept <mcusername>§6 or deny with §6§o/tpdeny§6 or §6§o/tpdeny <mcusername>§6."));

                //for some fucking reason player and target are swapped... this will play the sound on the target's side
                player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 2f, 0.7f );
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpahere command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpHereRequest(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();
            String targetPlayerName = StringArgumentType.getString(context, "target");

            ServerPlayerEntity target = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName)).getFirst();

            boolean success = TpxRequestManager.makeSingleTargetToPrivilegedSenderRequest(new SingleTargetToPrivilegedSenderRequest(player, target));
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException  e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tphere command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpHereAllRequest(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            boolean success = TpxRequestManager.makeMultiTargetsToSenderRequest(new MultipleTargetsToPrivilegedSenderRequest(player));
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tphereall command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpaDenyRequest(CommandContext<ServerCommandSource> context, boolean hasTargetArgument) {
        try {
            Optional<ServerPlayerEntity> target = Optional.empty();
            if ( hasTargetArgument ) {
                String targetPlayerName = StringArgumentType.getString(context, "target");
                List<ServerPlayerEntity> foundTargetPlayer = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName));
                if (!foundTargetPlayer.isEmpty()) {
                    target = Optional.of(foundTargetPlayer.getFirst());
                }
            }

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            if (target.isPresent() && player.getUuid().equals(target.get().getUuid())) {
                player.sendMessage(Text.literal("§o§9<Server>: Aw! You might deny yourself, but I'm never gonna give you up, never gonna let you down,"), false);
                player.sendMessage(Text.literal("§o§9Never gonna run around and desert you."), false);
                player.sendMessage(Text.literal("§o§9Never gonna make you cry, never gonna say goodbye." ), false);
                player.sendMessage(Text.literal("§o§9Never gonna tell a lie and hurt you."), false);
                return 1;
            }

            boolean success = TpxRequestManager.denySingleTpaRequest(player, target);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpdeny command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpaAcceptRequest(CommandContext<ServerCommandSource> context, boolean hasTargetArgument) {
        try {
            Optional<ServerPlayerEntity> sender = Optional.empty();
            if ( hasTargetArgument ) {
                String targetPlayerName = StringArgumentType.getString(context, "target");
                List<ServerPlayerEntity> foundTargetPlayer = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName));
                if (!foundTargetPlayer.isEmpty()) {
                    sender = Optional.of(foundTargetPlayer.getFirst());
                }
            }

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity receiver = source.getPlayerOrThrow();

            if (sender.isPresent() && receiver.getUuid().equals(sender.get().getUuid())) {
                receiver.sendMessage(Text.literal("§o§9<Server>: It's good to accept yourself."), false);
                return 1;
            }

            boolean success = TpxRequestManager.acceptSingleTpaRequest(receiver, sender);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpaccept command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpaCancelRequest(CommandContext<ServerCommandSource> context, boolean hasTargetArgument) {
        try {
            Optional<ServerPlayerEntity> target = Optional.empty();
            if ( hasTargetArgument ) {
                String targetPlayerName = StringArgumentType.getString(context, "target");
                List<ServerPlayerEntity> foundTargetPlayer = context.getSource().getWorld().getPlayers( playerEntity -> String.valueOf(playerEntity.getName().getString()).equals(targetPlayerName));
                if (!foundTargetPlayer.isEmpty()) {
                    target = Optional.of(foundTargetPlayer.getFirst());
                }
            }

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            if (target.isPresent() && player.getUuid().equals(target.get().getUuid())) {
                player.sendMessage(Text.literal("§o§9<Server>: Why would you want to cancel yourself? "), false);
                return 1;
            }

            boolean success = TpxRequestManager.cancelSingleTpaRequest(player, target);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Request failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpcancel command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpBlock(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            String targetPlayerName = StringArgumentType.getString(context, "target");

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            boolean success = TpxRequestManager.blockPlayerForPlayer(player, targetPlayerName, dbManager);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Blocking failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpblock command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpUnblock(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
            String targetPlayerName = StringArgumentType.getString(context, "target");

            ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            boolean success = TpxRequestManager.unblockPlayerForPlayer(targetPlayerName, player, dbManager);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch (TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Unblocking failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpunblock command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }

    private static int executeTpBlocklist(CommandContext<ServerCommandSource> context, DatabaseManager dbManager) {
        try {
           ServerCommandSource source = context.getSource();
            ServerPlayerEntity player = source.getPlayerOrThrow();

            boolean success = TpxRequestManager.getBlocklistOfPlayer(player, dbManager);
            if (success) {
                return 1;
            } else {
                return 0;
            }
        } catch ( TpxRequestAlreadyExistsException | TpxNotAllowedException | TpxRequestNotFoundException | NoSuchElementException | CommandSyntaxException e) {
            LOGGER.debug("Getting blocklist failed", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An error has occured: " + e.getMessage()), false));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected Exception: Failed to execute /tpblocklist command", e);
            Optional<ServerPlayerEntity> sender = Optional.ofNullable(context.getSource().getPlayer());
            sender.ifPresent(player -> player.sendMessage(Text.literal("§o§c An unknown error has occured."), false));
            return 0;
        }
    }
}
